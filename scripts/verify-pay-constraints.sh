#!/usr/bin/env bash
# 用真实插入验证支付表的防重约束。
# 应用层的"先查后插"在并发下挡不住重复支付，只有数据库唯一约束挡得住——
# 因此这里直接对约束做行为测试，而不是只检查索引是否存在。
set -uo pipefail
cd "$(dirname "$0")/.."
source scripts/lib/local-mysql.sh
# 导入本机 MySQL 的校验库 ccloud_verify（每次运行覆盖）
DB=ccloud_verify; fail=0; ERR=$(mktemp)
trap 'rm -f "$ERR"' EXIT

reset_db $DB
rename_db $DB < sql/mysql/ddl/05-pay.sql | lmysql $DB 2>/dev/null

try() {  # $1=说明 $2=期望(ok|dup) $3=SQL
  if lmysql $DB -e "$3" >/dev/null 2>"$ERR"; then got=ok; else got=dup; fi
  if [ "$got" = "$2" ]; then echo "  OK   $1"; else echo "  FAIL $1（期望 $2，实际 $got）"; cat "$ERR"; fail=1; fi
}
ORDER="INSERT INTO pay_order (app_id,merchant_order_id,subject,notify_url,price,status,user_ip,expire_time) VALUES"
try "首次创建商户单"                  ok  "$ORDER (1,'ORD-1','t','u',100,0,'1.1.1.1',NOW())"
try "同一应用重复商户单被拒绝"        dup "$ORDER (1,'ORD-1','t','u',100,0,'1.1.1.1',NOW())"
try "不同应用可用相同商户单号"        ok  "$ORDER (2,'ORD-1','t','u',100,0,'1.1.1.1',NOW())"

REFUND="INSERT INTO pay_refund (no,app_id,channel_id,channel_code,order_id,order_no,merchant_order_id,merchant_refund_id,notify_url,status,pay_price,refund_price,reason,channel_order_no) VALUES"
try "首次退款"                        ok  "$REFUND ('R1',1,1,'mock',1,'N1','ORD-1','RF-1','u',0,100,100,'r','C1')"
try "同一商户退款单重复退款被拒绝"    dup "$REFUND ('R2',1,1,'mock',1,'N1','ORD-1','RF-1','u',0,100,100,'r','C1')"

TX="INSERT INTO pay_wallet_transaction (no,wallet_id,biz_type,biz_id,title,price,balance) VALUES"
try "支付单首次扣款入账"              ok  "$TX ('T1',9,3,'P1','支付',-100,900)"
try "同一支付单重复扣款被拒绝"        dup "$TX ('T2',9,3,'P1','支付',-100,800)"
try "同一单据的退款流水可以入账"      ok  "$TX ('T3',9,4,'P1','退款',100,900)"

W="INSERT INTO pay_wallet (user_id,user_type,balance,total_expense,total_recharge,freeze_price) VALUES"
try "用户首个钱包"                    ok  "$W (7,1,0,0,0,0)"
try "同一用户重复开钱包被拒绝"        dup "$W (7,1,0,0,0,0)"

r=$(lmysql $DB -N -e "UPDATE pay_wallet SET balance=500 WHERE user_id=7;
  UPDATE pay_wallet SET balance=balance-800 WHERE user_id=7 AND balance>=800;
  SELECT ROW_COUNT(), (SELECT balance FROM pay_wallet WHERE user_id=7);" 2>/dev/null)
if [ "$r" = "$(printf '0\t500')" ]; then echo "  OK   余额不足时原子扣款影响 0 行，不会扣成负数"
else echo "  FAIL 原子扣款结果异常: $r"; fail=1; fi

[ $fail -eq 0 ] && echo "==> 支付防重约束全部生效" || exit 1

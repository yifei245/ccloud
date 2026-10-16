#!/usr/bin/env bash
# 端到端验收（P1 基础平台 + P2 会员运营 + P2 基础设施 + P2 开放平台 + P3 支付）：除会员内部接口外全部经网关调用。
# 前置：scripts/docker-up.sh 已启动全部服务。端口取自 docker/.env，可用同名环境变量覆盖。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/local-middleware.sh
source "$ROOT/scripts/lib/local-middleware.sh"

GW="http://127.0.0.1:${GATEWAY_PORT}"
TENANT='X-Tenant-Id: 1'
JSON='Content-Type: application/json'
ok() { echo "  OK   $1"; }
die() { echo "  FAIL $1"; exit 1; }
command -v jq >/dev/null || die "需要 jq"
curl -sf -o /dev/null "$GW/actuator/health" || die "网关不可访问（$GW），请先执行 scripts/docker-up.sh"

admin_login() {
  curl -s -X POST "$GW/admin-api/system/auth/login" -H "$JSON" -H "$TENANT" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}"
}

echo "==> 管理端（Web）"
LOGIN=$(admin_login admin admin123)
TOKEN=$(echo "$LOGIN" | jq -r '.data.accessToken // empty')
[ -n "$TOKEN" ] || die "admin 登录未拿到 accessToken：$LOGIN"; ok "admin 账号密码登录"
[ "$(admin_login admin wrong-password | jq -r '.code')" != "0" ] || die "错误密码登录成功"; ok "错误密码被拒绝"

PERM=$(curl -s "$GW/admin-api/system/auth/get-permission-info" -H "Authorization: Bearer $TOKEN")
echo "$PERM" | jq -e '.data.menus | length > 0' >/dev/null || die "菜单树为空：$(echo "$PERM" | head -c 200)"; ok "动态菜单"
echo "$PERM" | jq -e '.data.permissions | index("system:user:create")' >/dev/null || die "admin 缺少按钮权限 system:user:create"
ok "admin 拥有按钮级权限"

PLAIN=$(admin_login ccloud ccloud123 | jq -r '.data.accessToken // empty')
[ -n "$PLAIN" ] || die "ccloud 登录失败"
curl -s "$GW/admin-api/system/auth/get-permission-info" -H "Authorization: Bearer $PLAIN" \
  | jq -e '.data.permissions | index("system:user:create") | not' >/dev/null || die "普通管理员不应有 system:user:create"
# 用只需 URL 参数的删除接口：带请求体的接口会先做参数校验，返回 400 而到不了权限检查
R=$(curl -s -X DELETE "$GW/admin-api/system/user/delete?id=999999" -H "Authorization: Bearer $PLAIN" | jq -r '.code')
[ "$R" = "403" ] || die "普通管理员删除用户返回 $R，期望 403"; ok "普通管理员无删除用户权限（按钮级权限生效）"

R=$(curl -s "$GW/admin-api/system/user/page?pageNo=1&pageSize=10" -H "Authorization: Bearer $TOKEN" | jq -r '.code')
[ "$R" = "0" ] || die "admin 查询用户列表返回 $R"; ok "admin 查询用户列表"

echo "==> 网关安全"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$GW/admin-api/system/user/page?pageNo=1&pageSize=10")
[ "$CODE" = "401" ] || die "未登录访问返回 $CODE，期望 401"; ok "未登录拦截"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$GW/admin-api/system/user/page?pageNo=1&pageSize=10" \
  -H 'X-Login-User: eyJpZCI6MSwidXNlclR5cGUiOjIsInRlbmFudElkIjoxfQ==' -H 'X-Login-User-Sign: fake' \
  -H "X-Timestamp: $(date +%s)000")
[ "$CODE" = "401" ] || die "伪造内部身份头穿透，返回 $CODE"; ok "伪造内部身份头被拦截"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$GW/rpc-api/system/user/get?id=1")
[ "$CODE" = "403" ] || die "rpc-api 外部可访问，返回 $CODE"; ok "rpc-api 已阻断"
CODE=$(curl -s -o /dev/null -w '%{http_code}' --path-as-is "$GW/admin-api/system/..%2F..%2Frpc-api/system/user/get?id=1")
[ "$CODE" = "400" ] || die "路径穿越返回 $CODE，期望 400"; ok "路径穿越被拒绝"

echo "==> C 端（App / 小程序）"
member_login() {
  curl -s -X POST "$GW/app-api/member/auth/login" -H "$JSON" -H "$TENANT" -H "X-Client-Id: $1" \
    -d '{"mobile":"13800138000","password":"member123"}' | jq -r '.data.accessToken // empty'
}
MT=$(member_login ccloud-app)
[ -n "$MT" ] || die "C 端 App 登录失败"; ok "C 端 App 登录"
MINI=$(member_login ccloud-mp-weixin)
[ -n "$MINI" ] && [ "$MINI" != "$MT" ] || die "小程序客户端登录失败"; ok "小程序客户端登录（与 App 令牌互相独立）"
R=$(curl -s "$GW/app-api/member/user/get" -H "Authorization: Bearer $MT" | jq -r '.data.mobile')
[ "$R" = "13800138000" ] || die "会员查询本人信息返回 $R"; ok "会员查询本人信息"
R=$(curl -s "$GW/admin-api/system/user/page?pageNo=1&pageSize=10" -H "Authorization: Bearer $MT" | jq -r '.code')
[ "$R" != "0" ] || die "会员令牌越权访问管理端接口"; ok "会员令牌不能访问管理端"
R=$(curl -s "$GW/app-api/member/user/get" -H "Authorization: Bearer $TOKEN" | jq -r '.code')
[ "$R" != "0" ] || die "管理员令牌冒充会员"; ok "管理员令牌不能访问用户端"

echo "==> SSO 授权码"
AUTH=$(curl -s -G "$GW/admin-api/system/sso/authorize" -H "Authorization: Bearer $TOKEN" \
  --data-urlencode 'response_type=code' --data-urlencode 'client_id=ccloud-web' \
  --data-urlencode 'redirect_uri=http://localhost:8080/cb' --data-urlencode 'scope=user.read')
SSO_CODE=$(echo "$AUTH" | jq -r '.data.redirectUri // empty' | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
[ -n "$SSO_CODE" ] || die "SSO 未拿到授权码：$(echo "$AUTH" | head -c 300)"; ok "SSO 授权（自动授权范围）"
SSO=$(curl -s -X POST "$GW/admin-api/system/oauth2/token" -H "$TENANT" -u 'ccloud-web:ccloud-web-secret' \
  --data-urlencode 'grant_type=authorization_code' --data-urlencode "code=$SSO_CODE" \
  --data-urlencode 'redirect_uri=http://localhost:8080/cb')
SSO_T=$(echo "$SSO" | jq -r '.data.access_token // empty')
[ -n "$SSO_T" ] || die "授权码换令牌失败：$SSO"; ok "授权码换取令牌"
R=$(curl -s -X POST "$GW/admin-api/system/oauth2/token" -H "$TENANT" -u 'ccloud-web:ccloud-web-secret' \
  --data-urlencode 'grant_type=authorization_code' --data-urlencode "code=$SSO_CODE" \
  --data-urlencode 'redirect_uri=http://localhost:8080/cb' | jq -r '.code')
[ "$R" != "0" ] || die "授权码可重复使用"; ok "授权码只能使用一次"

echo "==> 令牌"
RT=$(admin_login admin admin123 | jq -r '.data.refreshToken')
NEW=$(curl -s -X POST "$GW/admin-api/system/auth/refresh-token?refreshToken=$RT" -H "$TENANT" | jq -r '.data.accessToken // empty')
[ -n "$NEW" ] || die "刷新令牌失败"; ok "刷新令牌"
curl -s -X POST "$GW/admin-api/system/auth/logout" -H "Authorization: Bearer $TOKEN" >/dev/null
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$GW/admin-api/system/auth/get-permission-info" -H "Authorization: Bearer $TOKEN")
[ "$CODE" = "401" ] || die "登出后令牌仍可用，返回 $CODE"; ok "登出后令牌立即失效"

echo "==> 会员运营"
AT=$(admin_login admin admin123 | jq -r '.data.accessToken')
MEMBER_ID=$(curl -s "$GW/admin-api/member/user/page?pageNo=1&pageSize=1&mobile=13800138000" -H "Authorization: Bearer $AT" \
  | jq -r '.data.list[0].id // empty')
[ -n "$MEMBER_ID" ] || die "管理端查不到演示会员"; ok "管理端查询会员"
member_point() { curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.point'; }
P0=$(member_point)
R=$(curl -s -X PUT "$GW/admin-api/member/user/update-point" -H "Authorization: Bearer $AT" -H "$JSON" \
  -d "{\"id\":$MEMBER_ID,\"point\":10}" | jq -r '.code')
[ "$R" = "0" ] || die "管理员加积分返回 $R"
P1=$(member_point)
[ "$P1" = "$((P0 + 10))" ] || die "加积分后为 $P1，期望 $((P0 + 10))"; ok "管理员修改积分"
R=$(curl -s -X PUT "$GW/admin-api/member/user/update-level" -H "Authorization: Bearer $AT" -H "$JSON" \
  -d "{\"id\":$MEMBER_ID,\"levelId\":2,\"reason\":\"e2e\"}" | jq -r '.code')
[ "$R" = "0" ] || die "管理员修改等级返回 $R"
[ "$(curl -s "$GW/admin-api/member/user/get?id=$MEMBER_ID" -H "Authorization: Bearer $AT" | jq -r '.data.levelName')" = "白银会员" ] \
  || die "修改等级后等级名称不是白银会员"; ok "管理员修改等级"
# 同一天重复执行脚本时已签到，两种结果都接受
R=$(curl -s -X POST "$GW/app-api/member/sign-in/record/create" -H "Authorization: Bearer $MT" | jq -r '.code')
[ "$R" = "0" ] || [ "$R" = "1004010000" ] || die "会员签到返回 $R"
curl -s "$GW/app-api/member/sign-in/record/get-summary" -H "Authorization: Bearer $MT" \
  | jq -e '.data.todaySignIn == true' >/dev/null || die "签到后概况显示今日未签到"; ok "会员签到"
curl -s "$GW/app-api/member/point/record/page?pageNo=1&pageSize=10" -H "Authorization: Bearer $MT" \
  | jq -e '.data.total > 0' >/dev/null || die "会员积分记录为空"; ok "会员查询积分记录"
curl -s "$GW/admin-api/member/level-record/page?pageNo=1&pageSize=10&userId=$MEMBER_ID" -H "Authorization: Bearer $AT" \
  | jq -e '.data.total > 0' >/dev/null || die "等级变更记录为空"; ok "管理端查询等级变更记录"
ADDR=$(curl -s -X POST "$GW/app-api/member/address/create" -H "Authorization: Bearer $MT" -H "$JSON" \
  -d '{"name":"张三","mobile":"13800138000","areaId":440305,"detailAddress":"科技园 1 号","defaultStatus":true}' | jq -r '.data // empty')
[ -n "$ADDR" ] || die "新增收件地址失败"
curl -s "$GW/app-api/member/address/list" -H "Authorization: Bearer $MT" \
  | jq -e --argjson id "$ADDR" 'any(.data[]; .id == $id)' >/dev/null || die "收件地址列表中没有新地址"
curl -s -X DELETE "$GW/app-api/member/address/delete?id=$ADDR" -H "Authorization: Bearer $MT" >/dev/null
ok "会员收件地址"
# 内部接口不经网关，直连 member 服务
MS="http://127.0.0.1:${MEMBER_PORT}"
BIZ="e2e-$(date +%s)"
BEFORE=$(member_point)
for _ in 1 2; do
  R=$(curl -s -X PUT "$MS/rpc-api/member/point/add?userId=$MEMBER_ID&point=5&bizType=10&bizId=$BIZ" -H "$TENANT" | jq -r '.code')
  [ "$R" = "0" ] || die "内部接口加积分返回 $R"
done
AFTER=$(member_point)
[ "$AFTER" = "$((BEFORE + 5))" ] || die "同一业务编号加积分两次后为 $AFTER，期望 $((BEFORE + 5))"
ok "内部接口加积分，同一业务编号只生效一次"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$GW/rpc-api/member/point/add?userId=$MEMBER_ID&point=5&bizType=10&bizId=x")
[ "$CODE" = "403" ] || die "经网关可访问会员内部接口，返回 $CODE"; ok "会员内部接口经网关已阻断"

echo "==> 基础设施"
TMP=$(mktemp -d)
AT=$(admin_login admin admin123 | jq -r '.data.accessToken')
AUTH="Authorization: Bearer $AT"
update_config() {  # $1=编号 $2=名称 $3=键 $4=值 $5=是否可见
  curl -s -X PUT "$GW/admin-api/infra/config/update" -H "$AUTH" -H "$JSON" \
    -d "{\"id\":$1,\"category\":\"biz\",\"name\":\"$2\",\"key\":\"$3\",\"value\":\"$4\",\"visible\":$5}" | jq -r '.code'
}
delete_admin_user() {  # $1=账号（精确匹配：分页接口的 username 参数是模糊查询）
  local id
  id=$(curl -s "$GW/admin-api/system/user/page?pageNo=1&pageSize=20&username=$1" -H "$AUTH" \
    | jq -r --arg u "$1" '[.data.list[] | select(.username == $u)][0].id // empty')
  if [ -n "$id" ]; then curl -s -X DELETE "$GW/admin-api/system/user/delete?id=$id" -H "$AUTH" >/dev/null; fi
}
REG="e2ereg$(date +%s)"
TID=""
MID=""
ERR_APP=""
ADMIN_AVATAR_SAVED=""
MEMBER_AVATAR_SAVED=""
# 无论本节在哪一步失败，都把改动过的参数恢复、删除创建的账号与测试数据
infra_cleanup() {
  set +e
  update_config 1 '用户管理-账号初始密码' system.user.init-password 123456 false >/dev/null
  update_config 2 '管理后台-是否开放注册' system.auth.register-enabled false true >/dev/null
  for u in demo01 demo02 "$REG" "${REG}x"; do delete_admin_user "$u"; done
  [ -z "$TID" ] || curl -s -X DELETE "$GW/admin-api/infra/codegen/delete?tableId=$TID" -H "$AUTH" >/dev/null
  [ -z "$MID" ] || lq "DELETE FROM ccloud.infra_message_dead_letter WHERE message_id='$MID';" >/dev/null
  [ -z "$ERR_APP" ] || lq "DELETE FROM ccloud.infra_api_error_log WHERE application_name='$ERR_APP';" >/dev/null
  # 还原头像（上传头像会改写用户资料）
  [ -z "$ADMIN_AVATAR_SAVED" ] || curl -s -X PUT "$GW/admin-api/system/user/profile/update" -H "$AUTH" -H "$JSON" \
    -d "{\"avatar\":\"$ADMIN_AVATAR\"}" >/dev/null
  [ -z "$MEMBER_AVATAR_SAVED" ] || curl -s -X PUT "$GW/app-api/member/user/update" -H "Authorization: Bearer $MT" -H "$JSON" \
    -d "{\"avatar\":\"$MEMBER_AVATAR\"}" >/dev/null
  redis_cli DEL ccloud:stream:e2e.topic >/dev/null 2>&1
  rm -rf "$TMP"
}
trap infra_cleanup EXIT
redis_cli() { docker exec "$REDIS_CONTAINER" redis-cli --no-auth-warning -a "$REDIS_PASSWORD" -n "$REDIS_DATABASE" "$@"; }


# 参数配置：导入用户使用参数中的初始密码
for u in demo01 demo02; do delete_admin_user "$u"; done
INIT_PW="E2e$(date +%s)"
[ "$(update_config 1 '用户管理-账号初始密码' system.user.init-password "$INIT_PW" false)" = "0" ] || die "修改初始密码参数失败"
curl -s "$GW/admin-api/system/user/get-import-template" -H "$AUTH" -o "$TMP/users.xls"
IMPORT=$(curl -s -X POST "$GW/admin-api/system/user/import" -H "$AUTH" -F "file=@$TMP/users.xls")
echo "$IMPORT" | jq -e '.data.createUsernames | index("demo01")' >/dev/null || die "导入用户失败：$IMPORT"
[ -n "$(admin_login demo01 "$INIT_PW" | jq -r '.data.accessToken // empty')" ] || die "导入的用户不能用参数中的初始密码登录"
ok "导入用户使用参数配置中的初始密码"
update_config 1 '用户管理-账号初始密码' system.user.init-password 123456 false >/dev/null
for u in demo01 demo02; do delete_admin_user "$u"; done

# 参数配置：注册开关
register() { curl -s -X POST "$GW/admin-api/system/auth/register" -H "$JSON" -H "$TENANT" \
  -d "{\"username\":\"$1\",\"nickname\":\"e2e\",\"password\":\"E2e123456\"}" | jq -r '.code'; }
[ "$(update_config 2 '管理后台-是否开放注册' system.auth.register-enabled true true)" = "0" ] || die "打开注册开关失败"
[ "$(register "$REG")" = "0" ] || die "注册开关打开后注册失败"
update_config 2 '管理后台-是否开放注册' system.auth.register-enabled false true >/dev/null
[ "$(register "${REG}x")" = "1001000010" ] || die "注册开关关闭后仍可注册"
delete_admin_user "$REG"
ok "注册开关由参数配置控制"

# 文件：上传、免登录读取、类型拦截、头像
python3 -c "import base64,sys; sys.stdout.buffer.write(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=='))" > "$TMP/a.png"
printf '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>' > "$TMP/a.svg"
URL=$(curl -s -X POST "$GW/admin-api/infra/file/upload" -H "$AUTH" -F "file=@$TMP/a.png" | jq -r '.data // empty')
[ -n "$URL" ] || die "管理端上传文件失败"
curl -s -D "$TMP/h" -o "$TMP/b.png" "$URL" || die "无法读取上传的文件：$URL（检查文件配置的访问域名是否指向网关）"
cmp -s "$TMP/a.png" "$TMP/b.png" || die "免登录读取的内容与上传的不一致：$URL"
grep -qi '^x-content-type-options: nosniff' "$TMP/h" || die "文件读取缺少 nosniff 头"
ok "上传文件并免登录读取"
R=$(curl -s -X POST "$GW/admin-api/infra/file/upload" -H "$AUTH" -F "file=@$TMP/a.svg;filename=a.png" | jq -r '.code')
[ "$R" = "1002007001" ] || die "SVG 改名后上传返回 $R，期望被拒绝"; ok "SVG 改名上传被拒绝"
R=$(curl -s -X POST "$GW/app-api/infra/file/upload" -H "Authorization: Bearer $MT" -F "file=@$TMP/a.png" | jq -r '.code')
[ "$R" = "0" ] || die "会员上传文件返回 $R"; ok "会员上传文件"
ADMIN_AVATAR=$(curl -s "$GW/admin-api/system/user/profile/get" -H "$AUTH" | jq -r '.data.avatar // ""'); ADMIN_AVATAR_SAVED=1
MEMBER_AVATAR=$(curl -s "$GW/app-api/member/user/get" -H "Authorization: Bearer $MT" | jq -r '.data.avatar // ""'); MEMBER_AVATAR_SAVED=1
AVATAR=$(curl -s -X PUT "$GW/admin-api/system/user/profile/update-avatar" -H "$AUTH" -F "avatarFile=@$TMP/a.png" | jq -r '.data // empty')
[ -n "$AVATAR" ] && [ "$(curl -s "$GW/admin-api/system/user/profile/get" -H "$AUTH" | jq -r '.data.avatar')" = "$AVATAR" ] \
  || die "管理员上传头像失败"
AVATAR=$(curl -s -X PUT "$GW/app-api/member/user/update-avatar" -H "Authorization: Bearer $MT" -F "avatarFile=@$TMP/a.png" | jq -r '.data // empty')
[ -n "$AVATAR" ] && [ "$(curl -s "$GW/app-api/member/user/get" -H "Authorization: Bearer $MT" | jq -r '.data.avatar')" = "$AVATAR" ] \
  || die "会员上传头像失败"
ok "管理员与会员上传头像"

# API 日志：登录请求已记录且密码已脱敏。用唯一账号发起一次登录（失败也会记访问日志），再按账号定位，不会被旧日志满足
LOG_USER="e2e_log_$(date +%s)_$$"
admin_login "$LOG_USER" admin123 >/dev/null
deadline=$((SECONDS + 60))
PARAMS=""
until PARAMS=$(curl -s "$GW/admin-api/infra/api-access-log/page?pageNo=1&pageSize=10&applicationName=ccloud-system-server&requestUrl=/admin-api/system/auth/login" -H "$AUTH" \
      | jq -r --arg u "$LOG_USER" '[.data.list[] | select((.requestParams // "") | contains($u))][0].requestParams // empty') \
      && [ -n "$PARAMS" ]; do
  [ $SECONDS -lt $deadline ] || die "60 秒内未查到账号 $LOG_USER 的登录访问日志"
  sleep 3
done
echo "$PARAMS" | grep -q 'admin123' && die "访问日志中出现了明文密码：$PARAMS"
echo "$PARAMS" | grep -q '\*\*\*\*\*\*' || die "访问日志中的密码未替换为掩码：$PARAMS"
ok "其他服务的访问日志经消息流写入 infra，密码已脱敏"
ERR_APP="e2e-$(date +%s)"
redis_cli XADD ccloud:stream:infra.api-error-log '*' messageId "$ERR_APP" \
  payload "{\"applicationName\":\"$ERR_APP\",\"tenantId\":1,\"exceptionName\":\"java.lang.IllegalStateException\"}" >/dev/null
deadline=$((SECONDS + 30))
until ERR_ID=$(curl -s "$GW/admin-api/infra/api-error-log/page?pageNo=1&pageSize=1&applicationName=$ERR_APP" -H "$AUTH" \
      | jq -r '.data.list[0].id // empty') && [ -n "$ERR_ID" ]; do
  [ $SECONDS -lt $deadline ] || die "30 秒内未查到异常日志"
  sleep 3
done
R=$(curl -s -X PUT "$GW/admin-api/infra/api-error-log/update-status?id=$ERR_ID&processStatus=1" -H "$AUTH" | jq -r '.code')
[ "$R" = "0" ] || die "标记异常日志为已处理返回 $R"; ok "异常日志消费写库并可处理"

# 数据源、数据库文档、代码生成
curl -s "$GW/admin-api/infra/data-source-config/list" -H "$AUTH" | jq -e '.data[0].id == 0' >/dev/null || die "数据源列表首位不是主数据源"
R=$(curl -s -X POST "$GW/admin-api/infra/data-source-config/create" -H "$AUTH" -H "$JSON" \
  -d '{"name":"E2E","url":"jdbc:mysql://host.docker.internal:3306/ccloud","username":"root","password":"wrong-password"}' | jq -r '.code')
[ "$R" = "1002003001" ] || die "连不上的数据源返回 $R，期望拒绝保存"; ok "数据源保存前测试连接"
curl -s -D "$TMP/h" "$GW/admin-api/infra/db-doc/export-html" -H "$AUTH" -o "$TMP/doc.html"
grep -q 'infra_demo_student' "$TMP/doc.html" && grep -qi "^content-security-policy: default-src 'none'" "$TMP/h" \
  || die "数据库文档内容或安全头不正确"; ok "数据库文档导出"
TID=$(curl -s -X POST "$GW/admin-api/infra/codegen/create-list" -H "$AUTH" -H "$JSON" \
  -d '{"dataSourceConfigId":0,"tableNames":["infra_demo_student"]}' | jq -r '.data[0] // empty')
[ -n "$TID" ] || die "代码生成导入演示表失败（若上次运行中断，先在管理端删除已导入的 infra_demo_student）"
N=$(curl -s "$GW/admin-api/infra/codegen/preview?tableId=$TID" -H "$AUTH" | jq -r '.data | length')
curl -s -X DELETE "$GW/admin-api/infra/codegen/delete?tableId=$TID" -H "$AUTH" >/dev/null; TID=""
[ "$N" = "11" ] || die "代码生成预览文件数为 $N，期望 11"; ok "代码生成导入与预览"

# 消息死信：重新投递到原主题
MID="e2e-dead-$(date +%s)"
lq "INSERT INTO ccloud.infra_message_dead_letter (message_id, topic, payload, retry_count, error_msg, handled, tenant_id)
    VALUES ('$MID', 'e2e.topic', '{\"a\":1}', 5, 'e2e', b'0', 1);" >/dev/null
DLID=$(lq "SELECT id FROM ccloud.infra_message_dead_letter WHERE message_id='$MID';")
[ -n "$DLID" ] || die "死信测试数据未写入"
R=$(curl -s -X PUT "$GW/admin-api/infra/message-dead-letter/resend?id=$DLID" -H "$AUTH" | jq -r '.code')
[ "$R" = "0" ] || die "死信重新投递返回 $R"
redis_cli XRANGE ccloud:stream:e2e.topic - + | grep -q "$MID" || die "死信未投递到原主题的消息流"
[ "$(lq "SELECT handled+0 FROM ccloud.infra_message_dead_letter WHERE id=$DLID;")" = "1" ] || die "重新投递后死信未标记为已处理"
ok "死信重新投递并标记为已处理"

echo "==> 开放平台"
OTMP=$(mktemp -d)
OPEN_APP_ID=""
RECV_PID=""
# 无论本节在哪一步失败，都停掉回调接收方、删除创建的应用及其日志
open_cleanup() {
  set +e
  if [ -n "$RECV_PID" ]; then kill "$RECV_PID" 2>/dev/null; wait "$RECV_PID" 2>/dev/null; fi
  if [ -n "$OPEN_APP_ID" ]; then
    curl -s -X DELETE "$GW/admin-api/open/app/delete?id=$OPEN_APP_ID" -H "$AUTH" >/dev/null
    lq "DELETE FROM ccloud.open_api_log WHERE app_id=$OPEN_APP_ID; DELETE FROM ccloud.open_notify_log WHERE app_id=$OPEN_APP_ID;" >/dev/null
  fi
  rm -rf "$OTMP"
}
trap 'open_cleanup; infra_cleanup' EXIT

# 回调接收方：记录请求头与请求体，按 $OTMP/status 文件应答
cat > "$OTMP/recv.py" <<'PY'
import http.server, json, os, sys
d = sys.argv[1]
class H(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        rec = {"headers": dict(self.headers), "body": body.decode()}
        with open(os.path.join(d, "last.json"), "w") as f:
            json.dump(rec, f)
        # 逐条追加：last.json 只留最后一次，会被重试中的旧回调覆盖，支付一段按 all.jsonl 查找事件
        with open(os.path.join(d, "all.jsonl"), "a") as f:
            f.write(json.dumps(rec) + "\n")
        code = int(open(os.path.join(d, "status")).read().strip() or 200) if os.path.exists(os.path.join(d, "status")) else 200
        self.send_response(code); self.end_headers(); self.wfile.write(b"success")
    def log_message(self, *a): pass
s = http.server.HTTPServer(("0.0.0.0", 0), H)
open(os.path.join(d, "port"), "w").write(str(s.server_port))
s.serve_forever()
PY
python3 "$OTMP/recv.py" "$OTMP" & RECV_PID=$!
for _ in $(seq 50); do [ -s "$OTMP/port" ] && break; sleep 0.2; done
[ -s "$OTMP/port" ] || die "回调接收方未能启动"
RECV_URL="http://host.docker.internal:$(cat "$OTMP/port")/notify"

# 新建应用，取一次性密钥，授权"查询会员信息"
R=$(curl -s -X POST "$GW/admin-api/open/app/create" -H "$AUTH" -H "$JSON" \
  -d "{\"name\":\"E2E开放应用\",\"status\":0,\"rateLimit\":50,\"notifyUrl\":\"$RECV_URL\"}")
OPEN_APP_ID=$(echo "$R" | jq -r '.data.id // empty'); APP_KEY=$(echo "$R" | jq -r '.data.appKey'); APP_SECRET=$(echo "$R" | jq -r '.data.appSecret')
[ -n "$OPEN_APP_ID" ] || die "新建开放应用失败：$R"
curl -s "$GW/admin-api/open/app/get?id=$OPEN_APP_ID" -H "$AUTH" | grep -q "$APP_SECRET" && die "应用详情返回了完整密钥"
API_ID=$(curl -s "$GW/admin-api/open/api/list-all-simple" -H "$AUTH" | jq -r '.data[] | select(.code=="member.user.get") | .id')
[ -n "$API_ID" ] || die "接口目录中没有 member.user.get"
# 请求体先放进变量：macOS 自带的 bash 3.2 会把"$(...)"里的 {a,b} 当作花括号展开而拆开参数
ASSIGN_BODY="{\"appId\":$OPEN_APP_ID,\"apiIds\":[$API_ID]}"
[ "$(curl -s -X PUT "$GW/admin-api/open/app/assign-api" -H "$AUTH" -H "$JSON" -d "$ASSIGN_BODY" | jq -r '.code')" = "0" ] \
  || die "授权接口失败"
ok "新建开放应用，密钥只返回一次，授权接口"

# 签名调用：$1=方法 $2=路径 $3=查询串 $4=appKey $5=密钥 $6=nonce（可选）。输出第一行为状态码，其后为响应体
open_call() {
  python3 - "$@" <<'PY'
import base64, hashlib, hmac, sys, time, uuid, urllib.request, urllib.error, os
m, path, q, key, secret = sys.argv[1:6]
nonce = sys.argv[6] if len(sys.argv) > 6 else uuid.uuid4().hex
ts = str(int(time.time() * 1000))
s = "\n".join([m, path, q, ts, nonce, hashlib.sha256(b"").hexdigest()])
sign = base64.b64encode(hmac.new(secret.encode(), s.encode(), hashlib.sha256).digest()).decode()
req = urllib.request.Request(os.environ["GW"] + path + ("?" + q if q else ""), method=m, headers={
    "X-Open-App-Id": key, "X-Open-Timestamp": ts, "X-Open-Nonce": nonce, "X-Open-Sign": sign})
try:
    r = urllib.request.urlopen(req); print(r.status); print(r.read().decode())
except urllib.error.HTTPError as e:
    print(e.code); print(e.read().decode())
PY
}
export GW
R=$(open_call GET /open-api/member/user/get "mobile=13800138000" "$APP_KEY" "$APP_SECRET")
[ "$(echo "$R" | head -1)" = "200" ] && [ "$(echo "$R" | tail -1 | jq -r '.data.mobile')" = "138****8000" ] \
  || die "签名调用开放接口失败：$R"
ok "签名调用开放接口，手机号已脱敏"

[ "$(open_call GET /open-api/member/user/get "mobile=13800138000" "$APP_KEY" wrong-secret | head -1)" = "401" ] || die "错误签名未被拒绝"
N="e2e-$(date +%s)"
open_call GET /open-api/member/user/get "mobile=13800138000" "$APP_KEY" "$APP_SECRET" "$N" >/dev/null
[ "$(open_call GET /open-api/member/user/get "mobile=13800138000" "$APP_KEY" "$APP_SECRET" "$N" | head -1)" = "401" ] || die "重放请求未被拒绝"
[ "$(open_call GET /open-api/member/user/page "" "$APP_KEY" "$APP_SECRET" | head -1)" = "403" ] || die "未授权的接口未返回 403"
ok "错误签名、重放 401，未授权接口 403"

# 应用变更经 Redis 广播使网关缓存失效，最多等 5 秒
open_status_is() {  # $1=期望的状态码 $2=密钥
  local i
  for i in 1 2 3 4 5 6 7 8 9 10; do
    [ "$(open_call GET /open-api/member/user/get "mobile=13800138000" "$APP_KEY" "$2" | head -1)" = "$1" ] && return 0
    sleep 0.5
  done
  return 1
}
update_open_app() {  # $1=状态
  local body="{\"id\":$OPEN_APP_ID,\"name\":\"E2E开放应用\",\"status\":$1,\"rateLimit\":50,\"notifyUrl\":\"$RECV_URL\"}"
  curl -s -X PUT "$GW/admin-api/open/app/update" -H "$AUTH" -H "$JSON" -d "$body" | jq -r '.code'
}
[ "$(update_open_app 1)" = "0" ] || die "停用开放应用失败"
open_status_is 401 "$APP_SECRET" || die "停用应用后仍可调用（网关缓存未失效）"
[ "$(update_open_app 0)" = "0" ] || die "重新开启开放应用失败"
open_status_is 200 "$APP_SECRET" || die "重新开启应用后仍无法调用"
NEW_SECRET=$(curl -s -X PUT "$GW/admin-api/open/app/reset-secret?id=$OPEN_APP_ID" -H "$AUTH" | jq -r '.data // empty')
[ -n "$NEW_SECRET" ] && [ "$NEW_SECRET" != "$APP_SECRET" ] || die "重置密钥失败"
open_status_is 401 "$APP_SECRET" || die "重置后旧密钥仍可用"
open_status_is 200 "$NEW_SECRET" || die "重置后新密钥不可用"
ok "停用与重置密钥立即生效"

deadline=$((SECONDS + 60))
until [ "$(curl -s "$GW/admin-api/open/api-log/page?pageNo=1&pageSize=10&appId=$OPEN_APP_ID" -H "$AUTH" \
    | jq -r '[.data.list[] | select(.apiCode=="member.user.get")] | length')" -gt 0 ]; do
  [ $SECONDS -lt $deadline ] || die "60 秒内未查到开放接口的调用日志"
  sleep 3
done
ok "开放接口调用日志经消息流写入 open"

# 测试回调：成功且可验签；对方失败时记为待重试
NID=$(curl -s -X POST "$GW/admin-api/open/app/test-notify?id=$OPEN_APP_ID" -H "$AUTH" | jq -r '.data // empty')
[ -n "$NID" ] || die "发送测试回调失败"
deadline=$((SECONDS + 30))
until [ "$(curl -s "$GW/admin-api/open/notify-log/get?id=$NID" -H "$AUTH" | jq -r '.data.status')" = "10" ]; do
  [ $SECONDS -lt $deadline ] || die "30 秒内测试回调未成功：$(curl -s "$GW/admin-api/open/notify-log/get?id=$NID" -H "$AUTH")"
  sleep 2
done
python3 - "$OTMP/last.json" "$NEW_SECRET" <<'PY' || die "回调验签失败"
import base64, hashlib, hmac, json, sys
d = json.load(open(sys.argv[1])); h = {k.lower(): v for k, v in d["headers"].items()}
s = h["x-open-timestamp"] + "\n" + h["x-open-event-id"] + "\n" + hashlib.sha256(d["body"].encode()).hexdigest()
assert base64.b64encode(hmac.new(sys.argv[2].encode(), s.encode(), hashlib.sha256).digest()).decode() == h["x-open-sign"]
assert json.loads(d["body"])["event"] == "open.test"
PY
echo 500 > "$OTMP/status"
NID=$(curl -s -X POST "$GW/admin-api/open/app/test-notify?id=$OPEN_APP_ID" -H "$AUTH" | jq -r '.data // empty')
[ -n "$NID" ] || die "发送第二次测试回调失败"
deadline=$((SECONDS + 30))
until [ "$(curl -s "$GW/admin-api/open/notify-log/get?id=$NID" -H "$AUTH" | jq -r '.data.status')" = "20" ]; do
  [ $SECONDS -lt $deadline ] || die "30 秒内失败的回调未记为待重试"
  sleep 2
done
ok "测试回调成功且可验签，对方失败时记为待重试"

echo "==> 支付"
# 开放平台一段把回调接收方的应答改成了 500，这里恢复；支付一段按 all.jsonl 查找回调
rm -f "$OTMP/status"
PAY_TS=$(date +%s)
PS="http://127.0.0.1:${PAY_PORT}"   # 内部接口不经网关，直连 pay 服务
PAY_APP_ID=""
PAY_CH_ID=""
PAY_STARTED=""
WID=""; WALLET_NEW=""; BAL0=0; TE0=0; TR0=0; RECHARGE0=0; TX0=0
# 无论本节在哪一步失败，都删除本节创建的支付应用、订单、退款、充值与流水，并把演示会员的钱包恢复原样。
# 只删本节创建的数据：支付应用按标识前缀 e2e-pay-，演示 mall 应用下按商户订单号前缀 E2E-，
# 充值记录与钱包流水按本节开始前记下的最大编号（时区不影响）
pay_cleanup() {
  set +e
  local e2e_apps="SELECT id FROM ccloud.pay_app WHERE app_key LIKE 'e2e-pay-%'"
  local mall_app="SELECT id FROM ccloud.pay_app WHERE app_key='mall' AND deleted=0"
  lq "DELETE FROM ccloud.pay_order_extension WHERE order_id IN (SELECT id FROM ccloud.pay_order WHERE app_id IN ($e2e_apps));
      DELETE FROM ccloud.pay_refund WHERE app_id IN ($e2e_apps);
      DELETE FROM ccloud.pay_order WHERE app_id IN ($e2e_apps);
      DELETE FROM ccloud.pay_channel WHERE app_id IN ($e2e_apps);
      DELETE FROM ccloud.pay_app WHERE app_key LIKE 'e2e-pay-%';
      DELETE FROM ccloud.pay_order_extension WHERE order_id IN
        (SELECT id FROM ccloud.pay_order WHERE app_id IN ($mall_app) AND merchant_order_id LIKE 'E2E-%');
      DELETE FROM ccloud.pay_refund WHERE app_id IN ($mall_app) AND merchant_order_id LIKE 'E2E-%';
      DELETE FROM ccloud.pay_order WHERE app_id IN ($mall_app) AND merchant_order_id LIKE 'E2E-%';
      DELETE FROM ccloud.open_notify_log WHERE payload LIKE '%\"merchantOrderId\":\"E2E-%'
        AND app_id IN (SELECT open_app_id FROM ccloud.pay_app WHERE app_key='mall' AND deleted=0);" >/dev/null
  if [ -n "$PAY_STARTED" ]; then
    local w="${WID:-$(lq "SELECT id FROM ccloud.pay_wallet WHERE user_id=$MEMBER_ID AND user_type=1 AND deleted=0")}"
    if [ -n "$w" ]; then
      local recharge_orders="SELECT pay_order_id FROM ccloud.pay_wallet_recharge WHERE id>$RECHARGE0 AND wallet_id=$w"
      lq "DELETE FROM ccloud.pay_order_extension WHERE order_id IN ($recharge_orders);
          DELETE FROM ccloud.pay_refund WHERE order_id IN ($recharge_orders);
          DELETE FROM ccloud.pay_order WHERE id IN ($recharge_orders);
          DELETE FROM ccloud.pay_wallet_recharge WHERE id>$RECHARGE0 AND wallet_id=$w;
          DELETE FROM ccloud.pay_wallet_transaction WHERE id>$TX0 AND wallet_id=$w;" >/dev/null
      if [ -n "$WALLET_NEW" ]; then
        lq "DELETE FROM ccloud.pay_wallet WHERE id=$w;" >/dev/null
      else
        lq "UPDATE ccloud.pay_wallet SET balance=$BAL0, total_expense=$TE0, total_recharge=$TR0 WHERE id=$w;" >/dev/null
      fi
    fi
  fi
}
trap 'pay_cleanup; open_cleanup; infra_cleanup' EXIT

# 记下演示会员的钱包与本节之前的最大编号，供清理时恢复
WID=$(lq "SELECT id FROM ccloud.pay_wallet WHERE user_id=$MEMBER_ID AND user_type=1 AND deleted=0")
if [ -n "$WID" ]; then
  read -r BAL0 TE0 TR0 <<<"$(lq "SELECT balance, total_expense, total_recharge FROM ccloud.pay_wallet WHERE id=$WID")"
else
  WALLET_NEW=1
fi
RECHARGE0=$(lq "SELECT COALESCE(MAX(id),0) FROM ccloud.pay_wallet_recharge")
TX0=$(lq "SELECT COALESCE(MAX(id),0) FROM ccloud.pay_wallet_transaction")
PAY_STARTED=1

# 带请求体的签名调用：$1=路径 $2=请求体 $3=appKey $4=密钥。签名串与 open_call 相同，最后一段为请求体的 SHA-256。
# 输出第一行为状态码，其后为响应体
open_post() {
  python3 - "$@" <<'PY'
import base64, hashlib, hmac, sys, time, uuid, urllib.request, urllib.error, os
path, body, key, secret = sys.argv[1:5]
data = body.encode()
nonce = uuid.uuid4().hex
ts = str(int(time.time() * 1000))
s = "\n".join(["POST", path, "", ts, nonce, hashlib.sha256(data).hexdigest()])
sign = base64.b64encode(hmac.new(secret.encode(), s.encode(), hashlib.sha256).digest()).decode()
req = urllib.request.Request(os.environ["GW"] + path, data=data, method="POST", headers={
    "Content-Type": "application/json", "X-Open-App-Id": key, "X-Open-Timestamp": ts, "X-Open-Nonce": nonce, "X-Open-Sign": sign})
try:
    r = urllib.request.urlopen(req); print(r.status); print(r.read().decode())
except urllib.error.HTTPError as e:
    print(e.code); print(e.read().decode())
PY
}
# 开放接口响应（open_call、open_post 的输出）：状态码为 200 且响应体满足 jq 条件
open_expect() {  # $1=响应 $2=jq 条件
  [ "$(echo "$1" | head -1)" = "200" ] && echo "$1" | tail -1 | jq -e "$2" >/dev/null 2>&1
}
# 轮询开放接口直到响应体满足 jq 条件：$1=路径 $2=查询串 $3=jq 条件 $4=超时秒数。最后一次响应留在 LAST_R
LAST_R=""
open_get_until() {
  local deadline=$((SECONDS + $4))
  until LAST_R=$(open_call GET "$1" "$2" "$APP_KEY" "$NEW_SECRET") && open_expect "$LAST_R" "$3"; do
    [ $SECONDS -lt $deadline ] || return 1
    sleep 2
  done
}
# 在回调接收方的记录里等待一条事件，并用开放应用的密钥验签：$1=事件 $2=data 中的键 $3=期望值 $4=超时秒数
await_notify() {
  python3 - "$OTMP/all.jsonl" "$NEW_SECRET" "$@" <<'PY'
import base64, hashlib, hmac, json, os, sys, time
path, secret, event, key, value, timeout = sys.argv[1:7]
deadline = time.time() + int(timeout)
while True:
    if os.path.exists(path):
        for line in open(path):
            try:
                d = json.loads(line); body = json.loads(d["body"])
            except ValueError:
                continue  # 接收方正在写这一行
            if body.get("event") != event or str((body.get("data") or {}).get(key)) != value:
                continue
            h = {k.lower(): v for k, v in d["headers"].items()}
            s = h["x-open-timestamp"] + "\n" + h["x-open-event-id"] + "\n" + hashlib.sha256(d["body"].encode()).hexdigest()
            good = base64.b64encode(hmac.new(secret.encode(), s.encode(), hashlib.sha256).digest()).decode()
            if good != h["x-open-sign"] or body.get("eventId") != h["x-open-event-id"]:
                print("回调验签失败或事件编号与请求头不符：" + d["body"]); sys.exit(2)
            sys.exit(0)
    if time.time() > deadline:
        print("超时未收到回调 %s %s=%s" % (event, key, value)); sys.exit(1)
    time.sleep(1)
PY
}

# ---- 外部系统：支付应用绑定开放应用，经开放平台下单、Mock 支付、收到回调 ----
PAY_KEY="e2e-pay-$PAY_TS"
R=$(curl -s -X POST "$GW/admin-api/pay/app/create" -H "$AUTH" -H "$JSON" \
  -d "{\"appKey\":\"$PAY_KEY\",\"name\":\"E2E支付应用\",\"status\":0,\"openAppKey\":\"$APP_KEY\"}")
PAY_APP_ID=$(echo "$R" | jq -r '.data // empty')
[ -n "$PAY_APP_ID" ] || die "新建支付应用失败：$R"
MOCK_CFG='{"notifySecret":"e2e-mock-notify-secret-01","behavior":"SUCCESS","delaySeconds":2}'
CH_BODY=$(jq -nc --argjson app "$PAY_APP_ID" --arg cfg "$MOCK_CFG" \
  '{appId:$app,code:"mock",status:0,feeRate:0,remark:"e2e",config:$cfg}')
R=$(curl -s -X POST "$GW/admin-api/pay/channel/create" -H "$AUTH" -H "$JSON" -d "$CH_BODY")
PAY_CH_ID=$(echo "$R" | jq -r '.data // empty')
[ -n "$PAY_CH_ID" ] || die "新建 Mock 渠道失败（pay 服务须开启 ccloud.pay.mock-enabled，docker 编排使用 dev 环境）：$R"
# 授权是整体替换：member.user.get 与 5 个 pay.* 接口一起提交
API_IDS=$(curl -s "$GW/admin-api/open/api/list-all-simple" -H "$AUTH" | jq -c \
  '[.data[] | select(.code | IN("member.user.get","pay.order.create","pay.order.get","pay.order.submit","pay.refund.create","pay.refund.get")) | .id]')
[ "$(echo "$API_IDS" | jq 'length')" = "6" ] || die "接口目录中缺少 member.user.get 或 pay.* 接口：$API_IDS"
ASSIGN_BODY="{\"appId\":$OPEN_APP_ID,\"apiIds\":$API_IDS}"
[ "$(curl -s -X PUT "$GW/admin-api/open/app/assign-api" -H "$AUTH" -H "$JSON" -d "$ASSIGN_BODY" | jq -r '.code')" = "0" ] \
  || die "授权支付接口失败"
# 授权变更经 Redis 广播使网关缓存失效，最多等 5 秒：查询一个不存在的订单，不再是 403 即已生效
for _ in 1 2 3 4 5 6 7 8 9 10; do
  [ "$(open_call GET /open-api/pay/order/get "merchantOrderId=E2E-none" "$APP_KEY" "$NEW_SECRET" | head -1)" != "403" ] && break
  sleep 0.5
done

OEXP=$(( ($(date +%s) + 3600) * 1000 ))   # 开放接口的时间是毫秒时间戳
P_ID="E2E-P$PAY_TS"
R=$(open_post /open-api/pay/order/create \
  "{\"merchantOrderId\":\"$P_ID\",\"subject\":\"E2E订单\",\"price\":100,\"expireTime\":$OEXP}" "$APP_KEY" "$NEW_SECRET")
open_expect "$R" ".code == 0 and .data.status == 0 and .data.merchantOrderId == \"$P_ID\" and .data.price == 100 and (.data.expireTime | type) == \"number\"" \
  || die "开放接口创建支付单失败：$R"
# 外部系统的订单没有付款会员，不能选钱包（先于订单与渠道校验）
R=$(open_post /open-api/pay/order/submit "{\"merchantOrderId\":\"$P_ID\",\"channelCode\":\"wallet\"}" "$APP_KEY" "$NEW_SECRET")
open_expect "$R" '.code == 1007002005' || die "开放接口选择钱包支付未被拒绝：$R"
R=$(open_post /open-api/pay/order/submit "{\"merchantOrderId\":\"$P_ID\",\"channelCode\":\"mock\"}" "$APP_KEY" "$NEW_SECRET")
open_expect "$R" '.code == 0 and .data.displayMode == "URL" and (.data.displayContent | startswith("/app-api/pay/mock/confirm?no="))' \
  || die "开放接口提交 Mock 支付失败：$R"
CONFIRM=$(echo "$R" | tail -1 | jq -r '.data.displayContent')
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW$CONFIRM")
[ "$CODE" = "200" ] || die "Mock 确认页确认支付返回 $CODE"
open_get_until /open-api/pay/order/get "merchantOrderId=$P_ID" '.data.status == 10 and .data.channelCode == "mock"' 30 \
  || die "30 秒内支付单未变为支付成功：$LAST_R"
NOTE=$(await_notify pay.order.paid merchantOrderId "$P_ID" 30) || die "未收到支付成功回调，或回调验签失败：$NOTE"
ok "外部系统经开放平台下单、Mock 支付、收到支付成功回调"

# ---- 退款 ----
RF_ID="E2E-R$PAY_TS"
R=$(open_post /open-api/pay/refund/create \
  "{\"merchantOrderId\":\"$P_ID\",\"merchantRefundId\":\"$RF_ID\",\"price\":30,\"reason\":\"e2e\"}" "$APP_KEY" "$NEW_SECRET")
open_expect "$R" ".code == 0 and .data.merchantRefundId == \"$RF_ID\" and .data.refundPrice == 30" || die "开放接口发起退款失败：$R"
open_get_until /open-api/pay/refund/get "merchantRefundId=$RF_ID" '.data.status == 10' 30 \
  || die "30 秒内退款未成功：$LAST_R"
NOTE=$(await_notify pay.refund.success merchantRefundId "$RF_ID" 30) || die "未收到退款成功回调，或回调验签失败：$NOTE"
open_get_until /open-api/pay/order/get "merchantOrderId=$P_ID" '.data.refundPrice == 30' 10 \
  || die "退款后支付单的已退款金额不是 30：$LAST_R"
ok "外部系统经开放平台退款并收到回调"

# ---- 会员充值：套餐 1 为"充 100 送 10"，到账 10000 + 1000 ----
MEMBER_AUTH="Authorization: Bearer $MT"
wallet_balance() { curl -s "$GW/app-api/pay/wallet/get" -H "$MEMBER_AUTH" | jq -r '.data.balance // empty'; }
R=$(curl -s -X POST "$GW/app-api/pay/wallet-recharge/create" -H "$MEMBER_AUTH" -H "$JSON" -d '{"packageId":1}')
RECHARGE_ORDER=$(echo "$R" | jq -r '.data.payOrderId // empty')
[ -n "$RECHARGE_ORDER" ] || die "创建充值失败：$R"
R=$(curl -s -X POST "$GW/app-api/pay/order/submit" -H "$MEMBER_AUTH" -H "$JSON" \
  -d "{\"id\":$RECHARGE_ORDER,\"channelCode\":\"mock\"}")
CONFIRM=$(echo "$R" | jq -r '.data.displayContent // empty')
[ -n "$CONFIRM" ] || die "充值订单提交 Mock 支付失败：$R"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW$CONFIRM")
[ "$CODE" = "200" ] || die "Mock 确认页确认充值返回 $CODE"
deadline=$((SECONDS + 60))
until [ "$(wallet_balance)" = "$((BAL0 + 11000))" ]; do
  [ $SECONDS -lt $deadline ] || die "60 秒内钱包余额未到 $((BAL0 + 11000))，当前 $(wallet_balance)"
  sleep 2
done
ok "会员充值经 Mock 付款后到账（含赠送）"

# ---- 钱包支付与退款：内部接口直连 pay 服务，会员以钱包付款 ----
W_ID="E2E-W$PAY_TS"
R=$(curl -s -X POST "$PS/rpc-api/pay/order/create" -H "$TENANT" -H "$JSON" \
  -d "{\"appKey\":\"mall\",\"merchantOrderId\":\"$W_ID\",\"subject\":\"E2E钱包支付\",\"price\":500,\"expireTime\":\"2099-01-01T00:00:00\",\"userIp\":\"127.0.0.1\",\"userId\":$MEMBER_ID,\"userType\":1}")
W_ORDER=$(echo "$R" | jq -r '.data // empty')
[ -n "$W_ORDER" ] || die "内部接口创建钱包订单失败：$R"
R=$(curl -s -X POST "$GW/app-api/pay/order/submit" -H "$MEMBER_AUTH" -H "$JSON" \
  -d "{\"id\":$W_ORDER,\"channelCode\":\"wallet\"}")
echo "$R" | jq -e '.code == 0 and .data.status == 10 and .data.displayMode == "NONE"' >/dev/null || die "钱包支付未即时成功：$R"
[ "$(wallet_balance)" = "$((BAL0 + 11000 - 500))" ] || die "钱包支付后余额为 $(wallet_balance)，期望 $((BAL0 + 11000 - 500))"
R=$(curl -s -X POST "$PS/rpc-api/pay/refund/create" -H "$TENANT" -H "$JSON" \
  -d "{\"appKey\":\"mall\",\"merchantOrderId\":\"$W_ID\",\"merchantRefundId\":\"${W_ID}-R\",\"price\":500,\"reason\":\"e2e\",\"userIp\":\"127.0.0.1\"}")
W_REFUND=$(echo "$R" | jq -r '.data // empty')
[ -n "$W_REFUND" ] || die "内部接口发起钱包退款失败：$R"
deadline=$((SECONDS + 30))
until [ "$(wallet_balance)" = "$((BAL0 + 11000))" ]; do
  [ $SECONDS -lt $deadline ] || die "30 秒内钱包退款未退回余额，当前 $(wallet_balance)"
  sleep 2
done
[ "$(curl -s "$PS/rpc-api/pay/refund/get?id=$W_REFUND" -H "$TENANT" | jq -r '.data.status')" = "10" ] || die "钱包退款单不是退款成功"
ok "钱包余额支付即时成功，退款退回余额"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/rpc-api/pay/order/create" -H "$JSON" -d '{}')
[ "$CODE" = "403" ] || die "经网关可访问支付内部接口，返回 $CODE"; ok "支付内部接口经网关已阻断"

# ---- 丢失回调由补查补回：渠道改为成功但不回调，补查任务每分钟执行，支付尝试创建 1 分钟后才查单 ----
UPD_BODY=$(jq -nc --argjson id "$PAY_CH_ID" --argjson app "$PAY_APP_ID" \
  --arg cfg '{"notifySecret":"***","behavior":"DROP_NOTIFY","delaySeconds":2}' \
  '{id:$id,appId:$app,code:"mock",status:0,feeRate:0,remark:"e2e",config:$cfg}')
R=$(curl -s -X PUT "$GW/admin-api/pay/channel/update" -H "$AUTH" -H "$JSON" -d "$UPD_BODY")
[ "$(echo "$R" | jq -r '.code')" = "0" ] || die "修改 Mock 渠道失败：$R"
CFG=$(curl -s "$GW/admin-api/pay/channel/get?id=$PAY_CH_ID" -H "$AUTH" | jq -r '.data.config')
echo "$CFG" | grep -q 'DROP_NOTIFY' && echo "$CFG" | grep -q '\*\*\*' && ! echo "$CFG" | grep -q 'e2e-mock-notify-secret-01' \
  || die "渠道配置未更新为 DROP_NOTIFY，或回显了明文密钥：$CFG"
D_ID="E2E-D$PAY_TS"
R=$(open_post /open-api/pay/order/create \
  "{\"merchantOrderId\":\"$D_ID\",\"subject\":\"E2E补查\",\"price\":100,\"expireTime\":$OEXP}" "$APP_KEY" "$NEW_SECRET")
open_expect "$R" '.code == 0 and .data.status == 0' || die "创建补查用支付单失败：$R"
R=$(open_post /open-api/pay/order/submit "{\"merchantOrderId\":\"$D_ID\",\"channelCode\":\"mock\"}" "$APP_KEY" "$NEW_SECRET")
open_expect "$R" '.code == 0 and .data.displayMode == "URL"' || die "补查用支付单提交失败：$R"
CONFIRM=$(echo "$R" | tail -1 | jq -r '.data.displayContent')
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW$CONFIRM")
[ "$CODE" = "200" ] || die "Mock 确认页确认补查用支付单返回 $CODE"
sleep 5
open_get_until /open-api/pay/order/get "merchantOrderId=$D_ID" '.data.status == 0' 1 \
  || die "渠道丢弃回调后支付单不应立即成功：$LAST_R"
open_get_until /open-api/pay/order/get "merchantOrderId=$D_ID" '.data.status == 10' 180 \
  || die "180 秒内补查任务未补回支付结果：$LAST_R"
NOTE=$(await_notify pay.order.paid merchantOrderId "$D_ID" 30) || die "补查补回支付结果后未收到支付成功回调，或回调验签失败：$NOTE"
ok "Mock 丢弃回调时由补查任务补回支付结果"

# ---- 生产保护：渠道回调入口免登录，靠渠道验签；不存在的渠道、错误签名都被拒绝 ----
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/app-api/pay/notify/order/999999999")
[ "$CODE" = "404" ] || die "不存在的渠道回调返回 $CODE，期望 404"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/app-api/pay/notify/order/$PAY_CH_ID" -H "$JSON" \
  -H "X-Mock-Timestamp: $(date +%s)000" -H "X-Mock-Sign: invalid" \
  -d '{"type":"order","no":"P0","status":"SUCCESS","price":100}')
[ "$CODE" = "400" ] || die "错误签名的渠道回调返回 $CODE，期望 400"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/app-api/pay/mock/confirm?no=P0")
[ "$CODE" = "404" ] || die "不存在的支付尝试确认返回 $CODE，期望 404"
ok "渠道回调：不存在的渠道 404，错误签名 400；Mock 确认页对不存在的支付 404"

echo "==> 文档、链路追踪、监控、定时任务"
curl -sf -o /dev/null "$GW/doc.html" || die "接口文档聚合页不可访问"
curl -s "$GW/system/v3/api-docs/admin" | jq -e '.paths | length > 0' >/dev/null || die "system 管理端文档为空"
curl -s "$GW/member/v3/api-docs/app" | jq -e '.paths | length > 0' >/dev/null || die "member 用户端文档为空"
curl -s "$GW/infra/v3/api-docs/admin" | jq -e '.paths | length > 0' >/dev/null || die "infra 管理端文档为空"
curl -s "$GW/open/v3/api-docs/admin" | jq -e '.paths | length > 0' >/dev/null || die "open 管理端文档为空"
ok "接口文档聚合"
curl -s -D- -o /dev/null "$GW/admin-api/system/auth/get-permission-info" -H "Authorization: Bearer $NEW" \
  | grep -qi '^trace-id:' || die "响应缺少 trace-id 头"; ok "响应携带 trace-id"
deadline=$((SECONDS + 60))
until curl -s "http://127.0.0.1:${ZIPKIN_PORT}/api/v2/services" | jq -e 'index("ccloud-system-server")' >/dev/null; do
  [ $SECONDS -lt $deadline ] || die "Zipkin 未收到 ccloud-system-server 的链路"
  sleep 5
done
ok "Zipkin 收到链路"
# 服务刚重启时（如 verify-nacos.sh --full 重启了 system），监控面板要过一个检查周期才标为 UP，最多等 60 秒
deadline=$((SECONDS + 60))
while true; do
  APPS=$(curl -s -u "admin:${CCLOUD_MONITOR_PASSWORD}" -H 'Accept: application/json' "http://127.0.0.1:${MONITOR_PORT}/applications")
  down=""
  for app in ccloud-gateway ccloud-system-server ccloud-infra-server ccloud-member-server ccloud-open-server ccloud-pay-server; do
    echo "$APPS" | jq -e --arg n "$app" 'map(select(.name == $n and .status == "UP")) | length == 1' >/dev/null || down="$down $app"
  done
  [ -z "$down" ] && break
  [ $SECONDS -lt $deadline ] || die "监控面板中$down 不是 UP"
  sleep 5
done
ok "监控面板显示 gateway、system、infra、member、open、pay 均为 UP"
JOBS=$(lq "SELECT COUNT(DISTINCT executor_handler) FROM xxl_job.xxl_job_log l JOIN xxl_job.xxl_job_group g ON l.job_group = g.id
  WHERE g.app_name = 'ccloud-system-server' AND l.handle_code = 200")
[ "${JOBS:-0}" -ge 2 ] || die "XXL-Job 两个重投任务尚未成功执行（成功的任务数 ${JOBS:-0}），任务每分钟调度一次，可稍后重试"
ok "XXL-Job 消息重投任务已按时执行"

echo "==> 端到端验收全部通过"

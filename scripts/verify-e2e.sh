#!/usr/bin/env bash
# 端到端验收（P1 基础平台 + P2 会员运营）：除会员内部接口外全部经网关调用。
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

echo "==> 文档、链路追踪、监控、定时任务"
curl -sf -o /dev/null "$GW/doc.html" || die "接口文档聚合页不可访问"
curl -s "$GW/system/v3/api-docs/admin" | jq -e '.paths | length > 0' >/dev/null || die "system 管理端文档为空"
curl -s "$GW/member/v3/api-docs/app" | jq -e '.paths | length > 0' >/dev/null || die "member 用户端文档为空"
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
  for app in ccloud-gateway ccloud-system-server ccloud-member-server; do
    echo "$APPS" | jq -e --arg n "$app" 'map(select(.name == $n and .status == "UP")) | length == 1' >/dev/null || down="$down $app"
  done
  [ -z "$down" ] && break
  [ $SECONDS -lt $deadline ] || die "监控面板中$down 不是 UP"
  sleep 5
done
ok "监控面板显示 gateway、system、member 均为 UP"
JOBS=$(lq "SELECT COUNT(DISTINCT executor_handler) FROM xxl_job.xxl_job_log l JOIN xxl_job.xxl_job_group g ON l.job_group = g.id
  WHERE g.app_name = 'ccloud-system-server' AND l.handle_code = 200")
[ "${JOBS:-0}" -ge 2 ] || die "XXL-Job 两个重投任务尚未成功执行（成功的任务数 ${JOBS:-0}），任务每分钟调度一次，可稍后重试"
ok "XXL-Job 消息重投任务已按时执行"

echo "==> 端到端验收全部通过"

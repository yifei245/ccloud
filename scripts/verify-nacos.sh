#!/usr/bin/env bash
# 校验 Nacos 配置包：导入后逐个读回，与仓库文件逐字一致；配置包中不得出现连接地址与密钥。
# 用法：scripts/verify-nacos.sh [--full]
#   --full  另外校验运行中的 system 服务确实加载了 Nacos 配置，且其优先级高于 application-{profile}.yaml
#           （需先 scripts/docker-up.sh 启动全部服务）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_DIR="$ROOT/docker/nacos/config"
# shellcheck source=lib/local-middleware.sh
source "$ROOT/scripts/lib/local-middleware.sh"
cd "$ROOT/docker"

FULL=false
[ "${1:-}" = "--full" ] && FULL=true

fail() { echo "FAIL $*"; exit 1; }

echo "==> 校验配置包不含连接地址与密钥"
[ -d "$CONFIG_DIR" ] || fail "缺少 $CONFIG_DIR"
# 只看非注释行、以这些词结尾的键名（如 password、app-secret、access-token、server-addr、url）；
# 键后第一个冒号紧跟 true / false 的开关（如 allow-private-address）不是地址或密钥，不计入；行尾注释里的 ": true" 不算
leaks=$(grep -HnEi '^[[:space:]]*-?[[:space:]]*[a-z0-9_.-]*(password|secret|token|url|uri|host|addr|address|addresses|username)[[:space:]]*:' \
  "$CONFIG_DIR"/*.yaml | grep -vEi '^[^:]*:[0-9]+:[[:space:]]*-?[[:space:]]*[a-z0-9_.-]+:[[:space:]]*(true|false)[[:space:]]*(#.*)?$' || true)
[ -z "$leaks" ] || fail "配置包含连接地址或密钥，这些应由各环境的 profile 与环境变量提供：
$leaks"

echo "==> 导入配置到本机 Nacos 的命名空间 $NACOS_NAMESPACE（覆盖已有内容）"
bash "$ROOT/scripts/nacos-import.sh" --overwrite

echo "==> 逐个读回校验"
token=$(nacos_token)
[ -n "$token" ] || fail "Nacos 登录失败"
count=0
for file in "$CONFIG_DIR"/*.yaml; do
  data_id=$(basename "$file")
  remote=$(curl -s -G "$NACOS/v3/admin/cs/config" -H "accessToken: $token" \
    --data-urlencode "dataId=$data_id" --data-urlencode "groupName=DEFAULT_GROUP" \
    --data-urlencode "namespaceId=$NACOS_NAMESPACE")
  # 用 python 解析 JSON 并与文件逐字比较，避免 shell 处理换行与转义出错
  if python3 - "$file" "$remote" <<'PY'
import json, sys
expected = open(sys.argv[1], encoding="utf-8").read()
data = json.loads(sys.argv[2]).get("data") or {}
sys.exit(0 if data.get("content") == expected and data.get("type") == "yaml" else 1)
PY
  then
    echo "    OK   $data_id"
    count=$((count + 1))
  else
    fail "$data_id 读回内容与文件不一致：$(echo "$remote" | head -c 200)"
  fi
done
[ "$count" -ge 5 ] || fail "配置文件数量不足：$count"

if $FULL; then
  echo "==> 重启 system，校验加载了 Nacos 配置且优先于 profile 文件"
  docker compose restart ccloud-system >/dev/null
  docker compose up -d --wait --wait-timeout 300 ccloud-system >/dev/null
  env_json=$(curl -s "http://127.0.0.1:${SYSTEM_PORT}/actuator/env")
  python3 - "$env_json" <<'PY' || fail "system 未按预期加载 Nacos 配置"
import json, sys
sources = [s["name"] for s in json.loads(sys.argv[1])["propertySources"]]
def index(keyword):
    return next((i for i, name in enumerate(sources) if keyword in name), None)
service, common, profile = index("ccloud-system-server.yaml"), index("ccloud-common.yaml"), index("application-dev.yaml")
print("    属性源顺序：", [s for s in sources if ".yaml" in s])
assert service is not None and common is not None, "未加载 Nacos 配置"
assert profile is not None, "未找到 application-dev.yaml"
# actuator 按优先级从高到低列出属性源：Nacos 须排在 profile 文件之前
assert service < profile and common < profile, "Nacos 配置的优先级低于 profile 文件"
PY

  echo "==> 校验标注为即时生效的配置发布后无需重启即生效（网关默认限流 100/s → 1/s）"
  # 配置属性端点默认隐藏属性值，这里用行为验证：阈值降到每秒 1 次后，连续请求应出现 429
  probe() {
    local codes=""
    for _ in 1 2 3 4 5; do
      codes="$codes $(curl -s -o /dev/null -w '%{http_code}' -G -H 'X-Tenant-Id: 1' \
        --data-urlencode 'name=ccloud 总部' "http://127.0.0.1:${GATEWAY_PORT}/admin-api/system/tenant/get-id-by-name")"
    done
    echo "$codes"
  }
  [[ "$(probe)" != *429* ]] || fail "修改前已出现限流，无法校验"
  modified=$(sed 's/^        replenish-rate: 100$/        replenish-rate: 1/; s/^        burst-capacity: 200$/        burst-capacity: 1/' \
    "$CONFIG_DIR/ccloud-gateway.yaml")
  [ "$modified" != "$(cat "$CONFIG_DIR/ccloud-gateway.yaml")" ] || fail "未找到默认限流阈值，无法校验"
  curl -s -X POST "$NACOS/v3/admin/cs/config" -H "accessToken: $token" \
    --data-urlencode "dataId=ccloud-gateway.yaml" --data-urlencode "groupName=DEFAULT_GROUP" \
    --data-urlencode "namespaceId=$NACOS_NAMESPACE" --data-urlencode "type=yaml" --data-urlencode "content=$modified" | grep -q '"code":0' || fail "发布修改失败"
  refreshed=false
  for _ in $(seq 1 15); do
    sleep 2
    [[ "$(probe)" == *429* ]] && { refreshed=true; break; }
  done
  # 无论结果如何都恢复为仓库中的内容
  bash "$ROOT/scripts/nacos-import.sh" --overwrite >/dev/null
  $refreshed || fail "发布后 30 秒内限流阈值未生效"
  sleep 5
  [[ "$(probe)" != *429* ]] || fail "恢复配置后仍在限流"
  echo "    已生效，并已恢复为仓库中的配置"
fi

echo "==> 通过"

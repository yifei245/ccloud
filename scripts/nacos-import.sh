#!/usr/bin/env bash
# 把 docker/nacos/config/ 下的配置导入 Nacos：dataId 即文件名，group 为 DEFAULT_GROUP，
# 命名空间取 NACOS_NAMESPACE（不存在时自动创建；为空则用 public）。
# 使用 Nacos 3 的管理接口 /nacos/v3/admin/cs/config（Nacos 3 已移除 v1、v2 的配置发布接口）。
#
# 用法：scripts/nacos-import.sh [--overwrite]
#   默认跳过 Nacos 中已存在的 dataId，避免覆盖运维在控制台改过的值；--overwrite 以仓库文件为准全部覆盖
# 环境变量：
#   NACOS_SERVER    Nacos 地址，默认 http://127.0.0.1:${NACOS_PORT:-8848}/nacos
#   NACOS_USERNAME / NACOS_PASSWORD   依次读取 ~/.ccloud/middleware.env、docker/.env
#   NACOS_NAMESPACE                   命名空间编号，默认读取 docker/.env
#   NACOS_CONFIG_DIR                  配置目录，默认仓库中的 docker/nacos/config
# 只依赖 bash 与 curl，也在 docker compose 的 ccloud-nacos-init 容器内执行
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# 个人文件先载入：与 .env 同名的键（如 NACOS_PASSWORD）以个人文件为准
for ENV_FILE in "$HOME/.ccloud/middleware.env" "$SCRIPT_DIR/../docker/.env"; do
  [ -f "$ENV_FILE" ] || continue
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" == \#* ]] && continue
    if [ -z "${!key+x}" ]; then export "$key=$value"; fi
  done < "$ENV_FILE"
done

OVERWRITE=false
[ "${1:-}" = "--overwrite" ] && OVERWRITE=true
NACOS_SERVER="${NACOS_SERVER:-http://127.0.0.1:${NACOS_PORT:-8848}/nacos}"
CONFIG_DIR="${NACOS_CONFIG_DIR:-$SCRIPT_DIR/../docker/nacos/config}"
GROUP=DEFAULT_GROUP
: "${NACOS_USERNAME:?必须设置 NACOS_USERNAME}"
: "${NACOS_PASSWORD:?必须设置 NACOS_PASSWORD}"

login=$(curl -fsS -X POST "$NACOS_SERVER/v3/auth/user/login" \
  --data-urlencode "username=$NACOS_USERNAME" --data-urlencode "password=$NACOS_PASSWORD")
token=$(printf '%s' "$login" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
[ -n "$token" ] || { echo "[nacos-import] 登录 Nacos 失败"; exit 1; }

NAMESPACE="${NACOS_NAMESPACE:-}"
NS_ARGS=()
if [ -n "$NAMESPACE" ]; then
  NS_ARGS=(--data-urlencode "namespaceId=$NAMESPACE")
  ns=$(curl -sS -G "$NACOS_SERVER/v3/admin/core/namespace" -H "accessToken: $token" \
    --data-urlencode "namespaceId=$NAMESPACE")
  if ! printf '%s' "$ns" | grep -q '"code":0'; then
    created=$(curl -sS -X POST "$NACOS_SERVER/v3/admin/core/namespace" -H "accessToken: $token" \
      --data-urlencode "namespaceId=$NAMESPACE" --data-urlencode "namespaceName=$NAMESPACE" \
      --data-urlencode "namespaceDesc=ccloud 配置与服务注册")
    printf '%s' "$created" | grep -q '"code":0' || { echo "[nacos-import] 创建命名空间 $NAMESPACE 失败：$created"; exit 1; }
    echo "[nacos-import] 已创建命名空间 $NAMESPACE"
  fi
fi

imported=0 skipped=0
for file in "$CONFIG_DIR"/*.yaml; do
  data_id=$(basename "$file")
  if ! $OVERWRITE; then
    existing=$(curl -sS -G "$NACOS_SERVER/v3/admin/cs/config" -H "accessToken: $token" \
      --data-urlencode "dataId=$data_id" --data-urlencode "groupName=$GROUP" ${NS_ARGS[@]+"${NS_ARGS[@]}"})
    if printf '%s' "$existing" | grep -q '"code":0'; then
      echo "[nacos-import] SKIP $data_id（已存在）"
      skipped=$((skipped + 1))
      continue
    fi
  fi
  # 首行注释作为配置描述
  desc=$(sed -n '1s/^#[[:space:]]*//p' "$file")
  result=$(curl -sS -X POST "$NACOS_SERVER/v3/admin/cs/config" -H "accessToken: $token" \
    --data-urlencode "dataId=$data_id" --data-urlencode "groupName=$GROUP" ${NS_ARGS[@]+"${NS_ARGS[@]}"} \
    --data-urlencode "type=yaml" --data-urlencode "desc=$desc" \
    --data-urlencode "content@$file")
  printf '%s' "$result" | grep -q '"code":0' || { echo "[nacos-import] 导入 $data_id 失败：$result"; exit 1; }
  echo "[nacos-import] OK   $data_id"
  imported=$((imported + 1))
done
echo "[nacos-import] 导入 $imported 个，跳过 $skipped 个"

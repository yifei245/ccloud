#!/usr/bin/env bash
# 停止 ccloud 的 docker 编排。
# 用法：
#   scripts/docker-down.sh          停止并删除容器，保留数据卷（MySQL、Redis 数据）
#   scripts/docker-down.sh --purge  同时删除数据卷，下次启动重新初始化数据库
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/docker"

if [ "${1:-}" = "--purge" ]; then
  docker compose down --volumes --remove-orphans
  echo "已停止并删除数据卷"
else
  docker compose down --remove-orphans
  echo "已停止（数据卷保留，彻底清理请加 --purge）"
fi

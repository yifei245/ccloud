#!/usr/bin/env bash
# 全量验收：依次执行各校验脚本，任一失败即中止。
# 连接本机已有的中间件（见 scripts/lib/local-middleware.sh），会打包并启动 ccloud 的 5 个应用（约 10 分钟）。
# 用法：scripts/verify-all.sh [--with-tests]   --with-tests 先执行 mvn test
set -euo pipefail
cd "$(dirname "$0")/.."

if [ "${1:-}" = "--with-tests" ]; then
  echo "######## mvn test"
  mvn -q test
fi
for step in "verify-deps.sh" "verify-framework.sh" "verify-sql.sh" "verify-codegen.sh" "verify-docs.sh" \
            "verify-docker.sh --full" "verify-nacos.sh --full" "verify-e2e.sh"; do
  echo "######## scripts/$step"
  # shellcheck disable=SC2086
  bash scripts/$step
done
echo "######## 全量验收通过"

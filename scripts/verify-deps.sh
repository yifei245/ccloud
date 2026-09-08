#!/usr/bin/env bash
# 验证 BOM 能解析、无版本冲突、且关键依赖落在预期版本上
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> 解析 BOM"
mvn -q -pl ccloud-dependencies validate

expect() {
  local prop="$1" want="$2" got
  got=$(mvn -q help:evaluate -Dexpression="$prop" -DforceStdout -pl ccloud-dependencies 2>/dev/null | tail -1)
  if [ "$got" = "$want" ]; then
    echo "  OK   $prop = $got"
  else
    echo "  FAIL $prop = '$got'，期望 '$want'"; exit 1
  fi
}
expect spring.boot.version        3.5.16
expect spring.cloud.version       2025.0.3
expect spring.cloud.alibaba.version 2025.0.0.0
expect mybatis-plus.version       3.5.17
expect redisson.version           3.52.0
expect springdoc.version          2.9.1
expect knife4j.version            4.5.0
expect xxl-job.version            3.4.2
expect spring-boot-admin.version  3.5.10

echo "==> 校验 BOM 中无 SNAPSHOT / RC / 快照后缀版本"
if grep -nE '<[a-z0-9.-]+\.version>.*(SNAPSHOT|-RC|-M[0-9]|\.B<)' ccloud-dependencies/pom.xml; then
  echo "  FAIL BOM 中存在非稳定版本"; exit 1
fi
echo "  OK   全部为稳定版本"
echo "==> 全部通过"

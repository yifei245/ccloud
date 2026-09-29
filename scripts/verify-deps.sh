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
echo "==> 校验 BouncyCastle 各模块同版本（混用会让 Hutool 初始化失败，加密字段全部不可用）"
# verbose 树会列出被仲裁掉的版本，任何一处残留旧版都能发现；需先 install 或在 reactor 内离线解析
bc=$(mvn -o dependency:tree -Dverbose -pl ccloud-module-system/ccloud-module-system-biz -am \
      -Dincludes=org.bouncycastle 2>/dev/null | grep -oE 'bc[a-z]+-jdk18on:jar:[0-9.]+:' | sort -u || true)
versions=$(echo "$bc" | awk -F: '{print $3}' | sort -u)
if [ "$(echo "$versions" | wc -l | tr -d ' ')" != "1" ]; then
  echo "  FAIL 存在多个版本："; echo "$bc"; exit 1
fi
echo "  OK   $(echo "$bc" | tr '\n' ' ')"
echo "==> 全部通过"

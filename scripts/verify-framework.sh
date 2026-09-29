#!/usr/bin/env bash
# framework 层整体校验：编译、测试、装配完整性、版本管理纪律
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> 编译并测试 framework 全部模块"
mvn -q -pl ccloud-framework -am test

echo "==> 校验每个 starter 都注册了 AutoConfiguration.imports"
missing=0
for d in ccloud-framework/ccloud-spring-boot-starter-*; do
  # starter-test 只提供测试基类，不需要自动配置
  [ "$(basename "$d")" = "ccloud-spring-boot-starter-test" ] && continue
  f="$d/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
  if [ ! -f "$f" ]; then
    echo "  FAIL 缺少 $f"; missing=1
  fi
done
[ $missing -eq 0 ] || exit 1
echo "  OK   全部 starter 均已注册自动配置"

echo "==> 校验 imports 中声明的类确实存在"
bad=0
while read -r line; do
  [ -z "$line" ] && continue
  cls="${line##*.}"
  if ! find ccloud-framework -path "*/main/java/*/${cls}.java" | grep -q .; then
    echo "  FAIL imports 中声明的 $line 找不到对应源文件"; bad=1
  fi
done < <(cat ccloud-framework/*/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 2>/dev/null)
[ $bad -eq 0 ] || exit 1
echo "  OK   自动配置类全部存在"

echo "==> 校验子模块 pom 中不出现硬编码 version（版本必须由 BOM 统一管理）"
if grep -rn "<version>" --include=pom.xml ccloud-framework \
     | grep -v "ccloud-framework/pom.xml" \
     | grep -v "<!--" \
     | grep -vE "<version>1\.0\.0</version>" ; then
  echo "  FAIL 子模块 pom 出现硬编码 version"; exit 1
fi
echo "  OK   版本全部由 BOM 管理"

echo "==> 校验框架层不依赖业务模块（依赖只能单向）"
# 只看 import 语句：@MapperScan 里的 com.ccloud.module 是扫描路径字符串，不是类依赖
if grep -rn "^import com\.ccloud\.module" --include=*.java ccloud-framework/*/src/main/java 2>/dev/null; then
  echo "  FAIL 框架层引用了业务模块的类"; exit 1
fi
echo "  OK   框架层未反向依赖业务模块"

echo "==> 全部通过"

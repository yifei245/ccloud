#!/usr/bin/env bash
# 代码生成验收：在临时 git 工作区里为两张演示表生成代码（学生表：管理后台单表；分类表：用户 App 树表），
# 合并错误码片段后编译 infra 并运行生成的测试。工作区基于当前提交（HEAD），未提交的改动不参与。
# 连接本机 MySQL / Redis（见 LocalMySql / LocalRedis），结束后删除工作区。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/ccloud-codegen-XXXXXX")"
REPO="$WORK/repo"
fail() { echo "  FAIL $*"; exit 1; }
cleanup() {
  git -C "$ROOT" worktree remove --force "$REPO" >/dev/null 2>&1 || true
  git -C "$ROOT" worktree prune >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

echo "==> 建立临时工作区（基于 $(git -C "$ROOT" rev-parse --short HEAD)）"
git -C "$ROOT" worktree add --detach "$REPO" HEAD >/dev/null
cd "$REPO"

echo "==> 生成两张演示表的代码"
CCLOUD_CODEGEN_OUTPUT="$REPO" mvn -o -q -pl ccloud-module-infra/ccloud-module-infra-biz -am test \
  -Dtest=CodegenOutputWriterTest -Dsurefire.failIfNoSpecifiedTests=false
generated=$(find ccloud-module-infra \( -name 'Demo*.java' -o -name 'AppDemo*.java' \) | wc -l | tr -d ' ')
[ "$generated" -ge 18 ] || fail "生成的 Java 文件只有 $generated 个"

echo "==> 合并错误码片段"
python3 - <<'PY'
import pathlib
enums = pathlib.Path("ccloud-module-infra/ccloud-module-infra-api/src/main/java/com/ccloud/module/infra/enums")
target = enums / "ErrorCodeConstants.java"
snippets = sorted(enums.glob("ErrorCodeConstants_*.java.txt"))
assert len(snippets) == 2, snippets
text = target.read_text(encoding="utf-8").rstrip()
assert text.endswith("}")
body = "\n".join(p.read_text(encoding="utf-8") for p in snippets)
target.write_text(text[:-1] + "\n" + body + "}\n", encoding="utf-8")
for p in snippets:
    p.unlink()
PY

echo "==> 编译并运行生成的测试"
mvn -o -q -pl ccloud-module-infra/ccloud-module-infra-biz -am test \
  -Dtest='DemoStudentServiceImplTest,DemoCategoryServiceImplTest' -Dsurefire.failIfNoSpecifiedTests=false
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET, sys
reports = glob.glob("ccloud-module-infra/ccloud-module-infra-biz/target/surefire-reports/TEST-*Demo*ServiceImplTest.xml")
if len(reports) != 2:
    sys.exit(f"  FAIL 生成的测试报告数量为 {len(reports)}，期望 2")
for report in reports:
    suite = ET.parse(report).getroot()
    tests, failures, errors = (int(suite.get(k)) for k in ("tests", "failures", "errors"))
    print(f"    {suite.get('name')}: {tests} 个用例，失败 {failures}，错误 {errors}")
    if tests == 0 or failures or errors:
        sys.exit("  FAIL 生成的测试未全部通过")
PY

echo "==> 代码生成验收通过"

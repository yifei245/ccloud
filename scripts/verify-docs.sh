#!/usr/bin/env bash
# 校验 README 与 docs/ 下的使用文档：必备小节、账号、版本号齐全；引用的文件与脚本真实存在；
# 访问地址中的端口与 docker/.env 一致。文档与实现漂移是常见问题，这里尽量用机器检查兜住
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0
need() { grep -qF -- "$1" README.md || { echo "  FAIL README 缺少：$1"; fail=1; }; }

echo "==> 校验 README 必备内容"
[ -f README.md ] || { echo "  FAIL 缺少 README.md"; exit 1; }
need "## 技术栈"; need "## 项目结构"; need "## 快速开始"; need "## 登录账号"
need "## 访问地址"; need "## 接口与鉴权"; need "## 数据库"; need "## 常见问题"; need "## License"
need "admin123"; need "ccloud123"; need "member123"; need "ccloud-open-demo"
need "Spring Boot"; need "3.5.16"; need "2025.0.3"; need "JDK 21"
need "X-Open-Sign"; need "docker/.env"

echo "==> 校验 LICENSE"
grep -q "MIT License" LICENSE || { echo "  FAIL LICENSE 非 MIT"; fail=1; }
grep -q "Permission is hereby granted, free of charge" LICENSE || { echo "  FAIL LICENSE 正文不完整"; fail=1; }

echo "==> 校验补充文档存在"
for doc in docs/快速开始.md docs/模块说明.md docs/接口鉴权说明.md; do
  [ -f "$doc" ] || { echo "  FAIL 缺少 $doc"; fail=1; }
done

echo "==> 校验文档引用的文件与脚本存在、端口与 docker/.env 一致"
python3 - README.md docs/快速开始.md docs/模块说明.md docs/接口鉴权说明.md <<'PY' || fail=1
import os, re, sys
ok = True
env = dict(line.split("=", 1) for line in open("docker/.env", encoding="utf-8").read().splitlines()
           if line and not line.startswith("#") and "=" in line)
for path in sys.argv[1:]:
    if not os.path.exists(path):
        continue
    text = open(path, encoding="utf-8").read()
    base = os.path.dirname(path)
    # 相对链接 [文字](路径)
    for target in re.findall(r"\]\((?!https?://|#|mailto:)([^)#\s]+)", text):
        if not os.path.exists(os.path.normpath(os.path.join(base, target))):
            print(f"  FAIL {path} 链接指向不存在的文件：{target}"); ok = False
    # 提到的脚本
    for script in set(re.findall(r"scripts/[\w-]+\.sh", text)):
        if not os.path.exists(script):
            print(f"  FAIL {path} 提到不存在的脚本：{script}"); ok = False
readme = open("README.md", encoding="utf-8").read()
for key in ("GATEWAY_PORT", "MONITOR_PORT", "XXL_JOB_PORT", "NACOS_CONSOLE_PORT", "NACOS_PORT", "ZIPKIN_PORT"):
    if f"127.0.0.1:{env[key]}" not in readme:
        print(f"  FAIL README 访问地址缺少 {key}={env[key]}（与 docker/.env 不一致）"); ok = False
# XXL-Job 管理员密码由 .env 设置，不再是上游的 123456
if env["XXL_JOB_ADMIN_PASSWORD"] not in readme or "123456" in readme:
    print("  FAIL README 中 XXL-Job 密码应为 docker/.env 的 XXL_JOB_ADMIN_PASSWORD"); ok = False
sys.exit(0 if ok else 1)
PY

[ $fail -eq 0 ] && echo "==> 文档校验通过" || exit 1

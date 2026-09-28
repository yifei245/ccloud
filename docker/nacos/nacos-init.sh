#!/bin/bash
# 初始化 Nacos 管理员账号 nacos 的密码（Nacos 3 不再内置默认账号）。已存在管理员时接口返回 409，视为成功。
# 不打印接口响应：成功时响应体会回显明文密码
set -euo pipefail

: "${NACOS_PASSWORD:?必须设置 NACOS_PASSWORD}"
response=$(curl -fsS -X POST "http://ccloud-nacos:8848/nacos/v3/auth/user/admin" \
  --data-urlencode "password=${NACOS_PASSWORD}")
code=$(printf '%s' "$response" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
case "$code" in
  0)   echo "[nacos-init] 已初始化管理员账号 nacos" ;;
  409) echo "[nacos-init] 管理员账号已存在，跳过" ;;
  *)   echo "[nacos-init] 初始化失败，返回码 ${code:-未知}"; exit 1 ;;
esac

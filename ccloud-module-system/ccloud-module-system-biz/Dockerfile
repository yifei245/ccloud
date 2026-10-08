# ccloud 应用镜像（gateway / system / infra / member / monitor 共用同一份内容，由 scripts/verify-docker.sh 校验一致）。
# 构建前先执行 mvn package；构建上下文为模块目录。
# 分层：依赖变动少、业务代码变动多，按层复制后只改代码时只需重建最后一层

# ---- 第一阶段：按 Spring Boot 分层工具拆开 fat jar ----
FROM eclipse-temurin:21-jre-alpine AS builder
ARG JAR_FILE=target/*.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /app/extracted

# ---- 第二阶段：运行镜像 ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && addgroup -S ccloud && adduser -S -G ccloud -h /app ccloud
WORKDIR /app
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
# /app/files 为本地磁盘文件存储的默认根目录（infra 使用），先建好并交给 ccloud 用户，挂载数据卷后仍可写
RUN mkdir -p /app/logs /app/files && chown -R ccloud:ccloud /app
# 不以 root 运行：应用被攻破时不直接拿到容器内 root
USER ccloud
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseZGC" \
    TZ=Asia/Shanghai
# exec 让 JVM 成为 1 号进程，docker stop 的 SIGTERM 能触发 Spring 优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.home=/app org.springframework.boot.loader.launch.JarLauncher"]

package com.ccloud.framework.test.core.ut;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 全部 Redis 测试共用本机已有的 Redis（默认 127.0.0.1:6379），不再为测试临时启动容器。
 *
 * <p>本机 Redis 与其他项目共用：测试只使用独立的库编号（默认 15），每个 JVM 首次使用时清空这一个库，
 * 保证与原先"每次新容器"一样从空库开始。禁止使用 0 号库，也禁止 FLUSHALL，避免清掉其他项目的数据。
 * 注意 Redis 的发布订阅不区分库编号，测试使用的频道名须带 ccloud 前缀或随机后缀。
 */
public final class LocalRedis {

    private static final String HOST = LocalMiddlewareConfig.get("ccloud.test.redis.host", "127.0.0.1");
    private static final int PORT = Integer.parseInt(LocalMiddlewareConfig.get("ccloud.test.redis.port", "6379"));
    private static final String PASSWORD = LocalMiddlewareConfig.get("ccloud.test.redis.password", "");
    private static final int DATABASE = Integer.parseInt(LocalMiddlewareConfig.get("ccloud.test.redis.database", "15"));

    private static volatile boolean cleaned;

    private LocalRedis() {
    }

    public static String host() {
        ensureClean();
        return HOST;
    }

    public static int port() {
        ensureClean();
        return PORT;
    }

    /**
     * @return 未设置密码时为 null
     */
    public static String password() {
        return PASSWORD.isEmpty() ? null : PASSWORD;
    }

    public static int database() {
        return DATABASE;
    }

    /**
     * 注册 Spring Data Redis 与 Redisson 共用的连接配置
     */
    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", LocalRedis::host);
        registry.add("spring.data.redis.port", LocalRedis::port);
        registry.add("spring.data.redis.password", PASSWORD::toString);
        registry.add("spring.data.redis.database", LocalRedis::database);
    }

    public static void ensureClean() {
        if (cleaned) {
            return;
        }
        synchronized (LocalRedis.class) {
            if (!cleaned) {
                flushTestDatabase(HOST, PORT, PASSWORD, DATABASE);
                cleaned = true;
            }
        }
    }

    /**
     * 直接按 RESP 协议发送命令，不依赖任何 Redis 客户端：本类也供不引入 Redis 客户端的模块使用
     */
    static void flushTestDatabase(String host, int port, String password, int database) {
        if (database == 0) {
            throw new IllegalStateException("测试不能使用 Redis 0 号库：本机 Redis 与其他项目共用，清库会误删其他项目的数据。"
                    + LocalMiddlewareConfig.HINT);
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            if (!password.isEmpty()) {
                command(out, in, "AUTH", password);
            }
            command(out, in, "SELECT", String.valueOf(database));
            command(out, in, "FLUSHDB");
        } catch (IOException e) {
            throw new IllegalStateException("无法连接本机 Redis " + host + ":" + port + "（" + e.getMessage()
                    + "）。请先启动本机的 Redis（如 docker start redis）。" + LocalMiddlewareConfig.HINT, e);
        }
    }

    private static void command(OutputStream out, BufferedReader in, String... args) throws IOException {
        StringBuilder request = new StringBuilder("*").append(args.length).append("\r\n");
        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            request.append('$').append(bytes.length).append("\r\n").append(arg).append("\r\n");
        }
        out.write(request.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
        String reply = in.readLine();
        if (reply == null || reply.startsWith("-")) {
            // 不回显命令参数，AUTH 的参数是密码
            throw new IllegalStateException("Redis 命令 " + args[0] + " 失败：" + reply + "。" + LocalMiddlewareConfig.HINT);
        }
    }
}

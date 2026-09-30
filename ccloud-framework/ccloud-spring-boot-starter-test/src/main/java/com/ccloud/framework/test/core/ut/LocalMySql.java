package com.ccloud.framework.test.core.ut;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 全部数据库测试共用本机已有的 MySQL（默认 127.0.0.1:3306），不再为测试临时启动容器。
 *
 * <p>测试使用独立的库（默认 {@code ccloud_test}），每个 JVM 首次使用时执行仓库中 {@code sql/mysql/ddl} 下的
 * 正式建表脚本（先删后建），保证与生产同一份表结构、且每次测试从空表开始。只动这一个库，不碰本机的其他库。
 */
public final class LocalMySql {

    private static final String HOST = LocalMiddlewareConfig.get("ccloud.test.mysql.host", "127.0.0.1");
    private static final String PORT = LocalMiddlewareConfig.get("ccloud.test.mysql.port", "3306");
    private static final String USERNAME = LocalMiddlewareConfig.get("ccloud.test.mysql.username", "root");
    private static final String PASSWORD = LocalMiddlewareConfig.get("ccloud.test.mysql.password", "");
    private static final String DATABASE = LocalMiddlewareConfig.get("ccloud.test.mysql.database", "ccloud_test");
    private static final String PARAMS = "?serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";

    /**
     * 建表脚本以 USE `ccloud` 切到正式库，测试时去掉，让脚本作用于测试库
     */
    private static final Pattern USE_DATABASE = Pattern.compile("(?im)^\\s*USE\\s+`?ccloud`?\\s*;\\s*$");

    private static volatile boolean initialized;

    private LocalMySql() {
    }

    public static String jdbcUrl() {
        ensureInitialized();
        return serverUrl() + DATABASE + PARAMS;
    }

    public static String username() {
        return USERNAME;
    }

    public static String password() {
        return PASSWORD;
    }

    public static String database() {
        return DATABASE;
    }

    /**
     * 把测试库注册为动态数据源的 master
     */
    public static void register(DynamicPropertyRegistry registry) {
        String prefix = "spring.datasource.dynamic.datasource.master.";
        registry.add(prefix + "url", LocalMySql::jdbcUrl);
        registry.add(prefix + "username", LocalMySql::username);
        registry.add(prefix + "password", LocalMySql::password);
        registry.add(prefix + "driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    public static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (LocalMySql.class) {
            if (!initialized) {
                initSchema();
                initialized = true;
            }
        }
    }

    private static String serverUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/";
    }

    private static void initSchema() {
        try (Connection connection = DriverManager.getConnection(serverUrl() + PARAMS, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + DATABASE
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        } catch (SQLException e) {
            throw new IllegalStateException("无法连接本机 MySQL " + HOST + ":" + PORT + "（" + e.getMessage()
                    + "）。请先启动本机的 MySQL（如 docker start mysql）。" + LocalMiddlewareConfig.HINT, e);
        }
        try (Connection connection = DriverManager.getConnection(serverUrl() + DATABASE + PARAMS, USERNAME, PASSWORD)) {
            for (Path file : listDdlFiles()) {
                String script = USE_DATABASE.matcher(Files.readString(file, StandardCharsets.UTF_8)).replaceAll("");
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8), file.toString()), StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("初始化测试库 " + DATABASE + " 失败", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 跳过负责建库的 00-schema.sql
     */
    static List<Path> listDdlFiles() {
        try (Stream<Path> files = Files.list(locateDdlDir())) {
            return files.filter(f -> f.getFileName().toString().endsWith(".sql"))
                    .filter(f -> !f.getFileName().toString().startsWith("00-"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 从当前工作目录（Maven 下为模块目录）逐级向上查找仓库根的 DDL 目录
     */
    public static Path locateDdlDir() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("sql/mysql/ddl");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("未找到 sql/mysql/ddl 目录");
    }
}

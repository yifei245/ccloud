package com.ccloud.framework.test.core.ut;

import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.io.support.EncodedResource;

/**
 * 全部数据库单测共用的 MySQL 容器（每个 JVM 只启动一次，进程退出时由 Testcontainers 回收）。
 *
 * <p>启动后执行仓库中 {@code sql/mysql/ddl} 下的正式建表脚本（跳过负责建库的 00-schema.sql，
 * 容器已建好 ccloud 库），保证测试与生产使用同一份表结构。
 */
public final class MySqlTestContainer {

    private static final String IMAGE = "mysql:8.4";

    private static volatile MySQLContainer<?> container;

    private MySqlTestContainer() {
    }

    public static MySQLContainer<?> get() {
        if (container == null) {
            synchronized (MySqlTestContainer.class) {
                if (container == null) {
                    MySQLContainer<?> c = new MySQLContainer<>(IMAGE)
                            .withDatabaseName("ccloud")
                            .withUrlParam("serverTimezone", "Asia/Shanghai")
                            .withUrlParam("characterEncoding", "UTF-8")
                            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");
                    c.start();
                    initSchema(c);
                    container = c;
                }
            }
        }
        return container;
    }

    private static void initSchema(MySQLContainer<?> c) {
        try (Connection connection = DriverManager.getConnection(c.getJdbcUrl(), c.getUsername(), c.getPassword())) {
            for (Path file : listDdlFiles()) {
                ScriptUtils.executeSqlScript(connection,
                        new EncodedResource(new FileSystemResource(file), StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("初始化测试库失败", e);
        }
    }

    static List<Path> listDdlFiles() {
        Path dir = locateDdlDir();
        try (Stream<Path> files = Files.list(dir)) {
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

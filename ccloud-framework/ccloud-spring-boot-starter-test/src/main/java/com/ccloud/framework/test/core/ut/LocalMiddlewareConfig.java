package com.ccloud.framework.test.core.ut;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * 测试连接本机已有中间件（MySQL、Redis）的配置。
 *
 * <p>取值优先级：环境变量（如 {@code CCLOUD_TEST_MYSQL_PASSWORD}）&gt; JVM 系统属性（如 {@code ccloud.test.mysql.password}）
 * &gt; {@code ~/.ccloud/test.properties} &gt; 默认值。密码只放在环境变量或用户目录的配置文件里，不进仓库。
 */
final class LocalMiddlewareConfig {

    static final Path FILE = Path.of(System.getProperty("user.home"), ".ccloud", "test.properties");

    static final String HINT = "连接配置可用环境变量 CCLOUD_TEST_MYSQL_{HOST,PORT,USERNAME,PASSWORD,DATABASE}、"
            + "CCLOUD_TEST_REDIS_{HOST,PORT,PASSWORD,DATABASE}，或写在 " + FILE + "（键如 ccloud.test.mysql.password）";

    private static final Properties FILE_PROPERTIES = load();

    private LocalMiddlewareConfig() {
    }

    static String get(String key, String defaultValue) {
        String env = System.getenv(key.toUpperCase(Locale.ROOT).replace('.', '_'));
        if (env != null && !env.isEmpty()) {
            return env;
        }
        String property = System.getProperty(key);
        if (property != null && !property.isEmpty()) {
            return property;
        }
        return FILE_PROPERTIES.getProperty(key, defaultValue);
    }

    private static Properties load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                properties.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("读取 " + FILE + " 失败", e);
            }
        }
        return properties;
    }
}

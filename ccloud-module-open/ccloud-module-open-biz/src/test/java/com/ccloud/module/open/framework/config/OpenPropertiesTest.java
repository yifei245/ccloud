package com.ccloud.module.open.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回调的 SSRF 防护开关：默认关闭，且基础配置与生产配置都不得打开（只允许本地开发与 docker 的 dev profile 打开）
 */
class OpenPropertiesTest {

    private static final String KEY = "ccloud.open.notify.allowprivateaddress";

    @Test
    void 默认不允许回调到内网地址() {
        assertThat(new OpenProperties().getNotify().isAllowPrivateAddress()).isFalse();
    }

    @Test
    void 基础配置与生产配置不打开内网回调() {
        for (String file : new String[]{"application.yaml", "application-prod.yaml"}) {
            Path path = Path.of("src/main/resources", file);
            assertThat(path).as(file).isRegularFile();
            Map<String, String> values = allowPrivateAddressValues(path);
            assertThat(values.values()).as(file + "：%s", values)
                    .allSatisfy(value -> assertThat(value.trim()).isEqualToIgnoringCase("false"));
        }
    }

    /**
     * 按 Spring 宽松绑定归一化键名（忽略 - 与大小写），找出所有指向该开关的配置项
     */
    private static Map<String, String> allowPrivateAddressValues(Path path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource(path));
        Properties properties = factory.getObject();
        assertThat(properties).isNotNull();
        // 值保留 YAML 的类型（布尔值不是 String），不能用 stringPropertyNames / getProperty
        return properties.keySet().stream().map(String::valueOf)
                .filter(key -> key.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT).equals(KEY))
                .collect(Collectors.toMap(key -> key, key -> String.valueOf(properties.get(key))));
    }
}

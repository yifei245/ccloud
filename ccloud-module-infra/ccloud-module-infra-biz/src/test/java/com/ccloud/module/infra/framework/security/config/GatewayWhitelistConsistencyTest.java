package com.ccloud.module.infra.framework.security.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关白名单与本服务匿名接口必须一致：网关少配，该接口经网关不可用；网关多配，网关失去一层保护。
 * 两份清单分处两个模块，只能靠测试防止漂移
 */
class GatewayWhitelistConsistencyTest {

    private static final List<String> PREFIXES = List.of("/admin-api/infra/", "/app-api/infra/");

    @Test
    void 网关白名单中本服务的条目应与本服务的匿名接口完全一致() throws IOException {
        Set<String> expected = new TreeSet<>();
        Stream.of(SecurityConfiguration.ANONYMOUS_URLS).forEach(expected::add);
        Stream.of(SecurityConfiguration.ANONYMOUS_GET_URLS).map(url -> "GET " + url).forEach(expected::add);

        Set<String> gateway = new TreeSet<>();
        for (String entry : gatewayIgnoreUrls()) {
            String path = entry.contains(" ") ? entry.substring(entry.indexOf(' ') + 1).trim() : entry;
            if (PREFIXES.stream().anyMatch(path::startsWith)) {
                gateway.add(entry.trim());
            }
        }

        assertThat(gateway).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    private static List<String> gatewayIgnoreUrls() throws IOException {
        try (InputStream in = Files.newInputStream(locateGatewayConfig())) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> ccloud = (Map<String, Object>) root.get("ccloud");
            Map<String, Object> security = (Map<String, Object>) ccloud.get("security");
            return (List<String>) security.get("ignore-urls");
        }
    }

    private static Path locateGatewayConfig() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("ccloud-gateway/src/main/resources/application.yaml");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("未找到 ccloud-gateway/src/main/resources/application.yaml");
    }
}

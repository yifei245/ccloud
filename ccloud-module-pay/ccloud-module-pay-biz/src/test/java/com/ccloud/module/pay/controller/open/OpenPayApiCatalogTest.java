package com.ccloud.module.pay.controller.open;

import com.ccloud.module.pay.test.BasePayWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 开放接口的路径与方法必须与种子数据里登记的接口目录（open_api 表）逐字一致：目录是外部系统开通权限的依据，
 * 路径漂移会让已授权的应用调不通，或让未登记的接口绕过授权
 */
class OpenPayApiCatalogTest extends BasePayWebIntegrationTest {

    private static final String PREFIX = "/open-api/pay/";
    // INSERT INTO `open_api` 的行：(id, 'name', 'code', 'path', 'METHOD', ...
    private static final Pattern ROW = Pattern.compile("\\(\\d+, '[^']*', '[^']*', '(" + PREFIX + "[^']+)', '(GET|POST)'");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void 开放接口与登记的接口目录一致() throws IOException {
        Set<String> registered = new TreeSet<>();
        Matcher matcher = ROW.matcher(Files.readString(locateDemoSql()));
        while (matcher.find()) {
            registered.add(matcher.group(2) + " " + matcher.group(1));
        }
        Set<String> implemented = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            for (String path : info.getPathPatternsCondition().getPatternValues()) {
                if (path.startsWith(PREFIX)) {
                    info.getMethodsCondition().getMethods().forEach(method -> implemented.add(method.name() + " " + path));
                }
            }
        }
        assertThat(registered).hasSize(5);
        assertThat(implemented).isEqualTo(registered);
    }

    private static Path locateDemoSql() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("sql/mysql/dml/07-demo.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("未找到 sql/mysql/dml/07-demo.sql");
    }
}

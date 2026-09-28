package com.ccloud.framework.swagger.config;

import com.ccloud.framework.common.enums.RpcConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiBuilderCustomizer;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.JavadocProvider;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.SecurityService;
import org.springdoc.core.utils.PropertyResolverUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger（springdoc + Knife4j）自动配置。
 *
 * <p>按接口族分组：admin / app / open / rpc。分组的意义在于，
 * 前端同学打开文档时只看自己那一组，不必在几百个接口里翻找。
 */
// 先于 Knife4j 的自动配置注册 knife4jOpenApiCustomizer，使其 @ConditionalOnMissingBean 不再创建原版
@AutoConfiguration(beforeName = "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration")
@ConditionalOnClass({OpenAPI.class})
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CcloudSwaggerAutoConfiguration {

    public static final String GROUP_ADMIN = "admin";
    public static final String GROUP_APP = "app";
    public static final String GROUP_OPEN = "open";
    public static final String GROUP_RPC = "rpc";

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    @Bean
    public OpenAPI ccloudOpenAPI(SwaggerProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion())
                        .contact(new Contact().name(properties.getAuthor())
                                .url(properties.getUrl()).email(properties.getEmail()))
                        .license(new License().name(properties.getLicense()).url(properties.getLicenseUrl())))
                .components(new Components().addSecuritySchemes(HttpHeaders_AUTHORIZATION,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .name(HttpHeaders_AUTHORIZATION)
                                .in(SecurityScheme.In.HEADER)
                                .description("登录后获得的 accessToken，格式：Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList(HttpHeaders_AUTHORIZATION));
    }

    private static final String HttpHeaders_AUTHORIZATION = "Authorization";

    /**
     * 替换 Knife4j 4.5.0 的 {@link Knife4jOpenApiCustomizer}：它按旧版 springdoc 编译，调用的
     * {@code SpringDocConfigProperties.getGroupConfigs()} 在 springdoc 2.9 中返回类型已由 List 改为 Set，
     * 生成任何分组文档都会抛 NoSuchMethodError。该定制器只负责给标签加排序扩展，去掉不影响文档内容。
     * Knife4j 发布兼容版本后可删除
     */
    @Bean
    @ConditionalOnClass(Knife4jOpenApiCustomizer.class)
    public Knife4jOpenApiCustomizer knife4jOpenApiCustomizer(SpringDocConfigProperties springDocConfigProperties) {
        return new Knife4jOpenApiCustomizer(new Knife4jProperties(), springDocConfigProperties) {
            @Override
            public void customise(OpenAPI openApi) {
                // 不做任何处理，见方法注释
            }
        };
    }

    @Bean
    public GroupedOpenApi adminGroupedOpenApi(SwaggerProperties properties) {
        return buildGroup(GROUP_ADMIN, RpcConstants.ADMIN_API_PREFIX + "/**");
    }

    @Bean
    public GroupedOpenApi appGroupedOpenApi(SwaggerProperties properties) {
        return buildGroup(GROUP_APP, RpcConstants.APP_API_PREFIX + "/**");
    }

    @Bean
    public GroupedOpenApi openGroupedOpenApi(SwaggerProperties properties) {
        return buildGroup(GROUP_OPEN, RpcConstants.OPEN_API_PREFIX + "/**");
    }

    @Bean
    @ConditionalOnProperty(prefix = "ccloud.swagger", name = "expose-rpc", havingValue = "true")
    public GroupedOpenApi rpcGroupedOpenApi() {
        return buildGroup(GROUP_RPC, RpcConstants.RPC_API_PREFIX + "/**");
    }

    /**
     * 每个分组都统一挂上租户 Header，避免多租户环境下调试时忘记传租户
     */
    private static GroupedOpenApi buildGroup(String group, String... pathsToMatch) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch(pathsToMatch)
                .addOperationCustomizer((operation, handlerMethod) -> {
                    List<Parameter> parameters = operation.getParameters() != null
                            ? operation.getParameters() : new ArrayList<>();
                    parameters.add(new Parameter()
                            .name(HEADER_TENANT_ID)
                            .description("租户编号")
                            .in(String.valueOf(SecurityScheme.In.HEADER).toLowerCase())
                            .schema(new StringSchema()._default("1").name(HEADER_TENANT_ID)));
                    operation.setParameters(parameters);
                    return operation;
                })
                .build();
    }
}

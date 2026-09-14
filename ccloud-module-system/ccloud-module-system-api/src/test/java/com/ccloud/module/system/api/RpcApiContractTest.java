package com.ccloud.module.system.api;

import com.ccloud.framework.common.enums.RpcConstants;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feign 接口的结构约束。这些约束违反时编译照过、单测照过，
 * 要到服务联调才以 404 或反序列化失败的形式出现。
 */
class RpcApiContractTest {

    private static List<Class<?>> apiInterfaces() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:com/ccloud/module/system/api/**/*Api.class");
        List<Class<?>> result = new ArrayList<>();
        for (Resource resource : resources) {
            String path = resource.getURL().getPath();
            String className = path.substring(path.indexOf("com/ccloud/")).replace('/', '.').replace(".class", "");
            Class<?> clazz = Class.forName(className);
            if (clazz.isInterface()) {
                result.add(clazz);
            }
        }
        return result;
    }

    @Test
    @DisplayName("应定义 plan 中约定的全部 RPC 接口")
    void shouldDefineAllApis() throws Exception {
        assertThat(apiInterfaces()).extracting(Class::getSimpleName).contains(
                "OAuth2TokenApi", "PermissionApi", "RoleApi", "AdminUserApi", "DeptApi", "PostApi",
                "DictDataApi", "SmsCodeApi", "SmsSendApi", "MailSendApi", "NotifyMessageSendApi",
                "SocialClientApi", "SocialUserApi", "TenantApi", "LoginLogApi", "OperateLogApi", "I18nMessageApi");
    }

    @Test
    @DisplayName("每个接口都应声明指向 system 服务的 @FeignClient，且不设置 path")
    void feignClientShouldTargetSystemServerWithoutPath() throws Exception {
        for (Class<?> api : apiInterfaces()) {
            FeignClient feignClient = api.getAnnotation(FeignClient.class);
            assertThat(feignClient).as(api.getSimpleName() + " 缺少 @FeignClient").isNotNull();
            assertThat(feignClient.name()).as(api.getSimpleName()).isEqualTo(ApiConstants.NAME);
            // path 不会被服务端实现类继承，设了它会造成调用方与服务端路径不一致
            assertThat(feignClient.path()).as(api.getSimpleName() + " 不应设置 @FeignClient.path").isEmpty();
        }
    }

    @Test
    @DisplayName("每个方法都应有请求映射，路径以 /rpc-api/system 开头，且返回 CommonResult")
    void methodsShouldBeMappedUnderRpcPrefixAndReturnCommonResult() throws Exception {
        for (Class<?> api : apiInterfaces()) {
            for (Method method : api.getDeclaredMethods()) {
                if (method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                String where = api.getSimpleName() + "#" + method.getName();
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                assertThat(mapping).as(where + " 缺少请求映射").isNotNull();
                assertThat(mapping.path()).as(where).hasSize(1);
                assertThat(mapping.path()[0]).as(where).startsWith(RpcConstants.RPC_API_PREFIX + "/system/");
                assertThat(mapping.method()).as(where + " 需声明 HTTP 方法").hasSize(1);
                assertThat(method.getReturnType()).as(where).isEqualTo(CommonResult.class);
            }
        }
    }

    @Test
    @DisplayName("全部接口的 HTTP 方法加路径不得重复，否则服务端启动时映射冲突")
    void mappingsShouldBeUnique() throws Exception {
        Map<String, String> seen = new HashMap<>();
        for (Class<?> api : apiInterfaces()) {
            for (Method method : api.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String key = mapping.method()[0] + " " + mapping.path()[0];
                String where = api.getSimpleName() + "#" + method.getName();
                assertThat(seen.putIfAbsent(key, where)).as(key + " 与 " + where + " 冲突").isNull();
            }
        }
    }

    @Test
    @DisplayName("方法参数应显式声明绑定方式，避免 Feign 默认把无注解参数当作请求体")
    void parametersShouldBeExplicitlyBound() throws Exception {
        for (Class<?> api : apiInterfaces()) {
            for (Method method : api.getDeclaredMethods()) {
                String where = api.getSimpleName() + "#" + method.getName();
                var annotations = method.getParameterAnnotations();
                long bodyCount = 0;
                for (int i = 0; i < annotations.length; i++) {
                    boolean bound = Arrays.stream(annotations[i]).anyMatch(a ->
                            a.annotationType().getPackageName().equals("org.springframework.web.bind.annotation"));
                    assertThat(bound).as(where + " 第 " + (i + 1) + " 个参数缺少绑定注解").isTrue();
                    bodyCount += Arrays.stream(annotations[i])
                            .filter(a -> a.annotationType().getSimpleName().equals("RequestBody")).count();
                }
                assertThat(bodyCount).as(where + " 最多一个 @RequestBody").isLessThanOrEqualTo(1);
            }
        }
    }
}

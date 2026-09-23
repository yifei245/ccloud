package com.ccloud.module.member.api;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * member-api 的每个 Feign 接口要么已有实现，要么登记在待实现清单中
 */
class RpcApiCoverageTest {

    /**
     * 随对应任务提供实现。实现落地后须从此处移除，否则本测试失败
     */
    private static final Map<String, String> PENDING_APIS = Map.of(
            "MemberUserApi", "Task 40",
            "MemberAddressApi", "P2 商城", "MemberLevelApi", "P2 商城",
            "MemberExperienceApi", "P2 商城", "MemberPointApi", "P2 商城");

    @Test
    void 每个Feign接口都已实现或登记为待实现() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
        Set<String> pending = new TreeSet<>(PENDING_APIS.keySet());
        int apis = 0;
        for (var resource : resolver.getResources("classpath*:com/ccloud/module/member/api/**/*Api.class")) {
            var metadata = readerFactory.getMetadataReader(resource).getAnnotationMetadata();
            if (!metadata.isInterface() || !metadata.hasAnnotation(FeignClient.class.getName())) {
                continue;
            }
            apis++;
            Class<?> api = Class.forName(metadata.getClassName());
            Class<?> impl;
            try {
                impl = Class.forName(api.getName() + "Impl");
            } catch (ClassNotFoundException e) {
                assertThat(PENDING_APIS).as("%s 缺少实现", api.getSimpleName()).containsKey(api.getSimpleName());
                continue;
            }
            assertThat(pending.remove(api.getSimpleName())).as("%s 已实现，应从 PENDING_APIS 中移除", api.getSimpleName()).isFalse();
            assertThat(impl.isAnnotationPresent(RestController.class)).as(impl.getName()).isTrue();
            for (Method method : api.getMethods()) {
                if (!method.isDefault() && !Modifier.isStatic(method.getModifiers())) {
                    assertThat(impl.getMethod(method.getName(), method.getParameterTypes()).getDeclaringClass()).isEqualTo(impl);
                }
            }
        }
        assertThat(apis).isEqualTo(5);
    }
}

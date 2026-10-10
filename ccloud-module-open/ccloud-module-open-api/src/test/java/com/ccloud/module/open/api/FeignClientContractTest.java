package com.ccloud.module.open.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeignClientContractTest {

    @Test
    @DisplayName("每个 Feign 接口都须声明唯一的 contextId——同名客户端在调用方注册多个时会冲突，服务直接起不来")
    void everyClientShouldHaveUniqueContextId() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
        List<String> contextIds = new ArrayList<>();
        for (var resource : resolver.getResources("classpath*:com/ccloud/module/open/api/**/*Api.class")) {
            var metadata = readerFactory.getMetadataReader(resource).getAnnotationMetadata();
            if (!metadata.hasAnnotation(FeignClient.class.getName())) {
                continue;
            }
            FeignClient client = Class.forName(metadata.getClassName()).getAnnotation(FeignClient.class);
            assertThat(client.contextId()).as(metadata.getClassName()).isNotBlank();
            contextIds.add(client.contextId());
        }
        assertThat(contextIds).hasSize(2).doesNotHaveDuplicates();
    }
}

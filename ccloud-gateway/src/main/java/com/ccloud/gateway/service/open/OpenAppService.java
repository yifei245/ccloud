package com.ccloud.gateway.service.open;

import reactor.core.publisher.Mono;

public interface OpenAppService {

    /**
     * 按 appKey 获得应用快照
     *
     * @return 应用存在时为快照；不存在时为空
     * @throws OpenAppUnavailableException 以 Mono.error 发出：open 不可用或返回了非预期结果，
     *                                     不能据此判定应用不存在
     */
    Mono<OpenApp> getApp(String appKey);

    /**
     * 清除应用的本地缓存，收到 open 的失效广播后调用
     */
    void invalidate(String appKey);

    /**
     * 清除全部应用的本地缓存
     */
    void invalidateAll();
}

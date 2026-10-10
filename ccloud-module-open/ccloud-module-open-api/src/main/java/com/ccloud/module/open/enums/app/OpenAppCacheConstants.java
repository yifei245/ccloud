package com.ccloud.module.open.enums.app;

/**
 * 开放应用缓存常量
 */
public interface OpenAppCacheConstants {

    /**
     * 应用缓存失效频道。消息体为 appKey，空串表示全部失效。
     * 网关 OpenAppInvalidationListener.CHANNEL 须与此逐字一致
     */
    String INVALIDATE_CHANNEL = "ccloud:open:app:invalidate";
}

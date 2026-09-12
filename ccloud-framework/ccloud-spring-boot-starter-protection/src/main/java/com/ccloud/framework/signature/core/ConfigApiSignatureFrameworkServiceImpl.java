package com.ccloud.framework.signature.core;

import com.ccloud.framework.signature.config.ApiSignatureProperties;
import lombok.RequiredArgsConstructor;

/**
 * 从配置读取密钥的兜底实现。
 *
 * <p>P1 阶段 open 模块尚未实现，用它让开放 API 能在本地自测；
 * P2 接入后由 open 模块的 RPC 实现覆盖本 Bean。
 */
@RequiredArgsConstructor
public class ConfigApiSignatureFrameworkServiceImpl implements ApiSignatureFrameworkService {

    private final ApiSignatureProperties properties;

    @Override
    public String getAppSecret(String appKey) {
        return properties.getApps().get(appKey);
    }
}

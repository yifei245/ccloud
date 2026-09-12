package com.ccloud.framework.signature.core;

/**
 * 开放应用密钥查询契约。实现由 open 模块经 RPC 提供（P2）。
 */
public interface ApiSignatureFrameworkService {

    /**
     * @return appKey 对应的密钥；应用不存在、已禁用或已过期时返回 null
     */
    String getAppSecret(String appKey);
}

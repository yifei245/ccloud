package com.ccloud.gateway.service.auth;

import com.ccloud.gateway.filter.security.LoginUser;
import reactor.core.publisher.Mono;

public interface TokenCheckService {

    /**
     * 校验访问令牌
     *
     * @return 令牌有效时为登录用户；令牌无效或过期时为空
     * @throws TokenCheckUnavailableException 以 Mono.error 发出：system 不可用或返回了非预期结果，
     *                                        不能据此判定令牌无效
     */
    Mono<LoginUser> checkAccessToken(String accessToken);

    /**
     * 清除令牌的本地缓存，登出后调用
     */
    void invalidate(String accessToken);
}

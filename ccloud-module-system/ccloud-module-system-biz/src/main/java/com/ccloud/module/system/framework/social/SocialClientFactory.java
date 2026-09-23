package com.ccloud.module.system.framework.social;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaRedisBetterConfigImpl;
import cn.hutool.core.util.StrUtil;
import com.ccloud.module.system.dal.dataobject.social.SocialClientDO;
import com.ccloud.module.system.dal.redis.social.SocialAuthStateRedisDAO;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 按社交客户端配置创建平台 SDK 客户端。与平台的网络交互都经由这里创建的对象，测试时在此替换
 */
@Component
public class SocialClientFactory {

    private final SocialAuthStateRedisDAO authStateCache;
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 小程序客户端按 appId + 密钥缓存：access_token 由它维护，每次新建会反复向微信换取
     */
    private final Map<String, WxMaService> wxMaServices = new ConcurrentHashMap<>();

    public SocialClientFactory(SocialAuthStateRedisDAO authStateCache, StringRedisTemplate stringRedisTemplate) {
        this.authStateCache = authStateCache;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * @param redirectUri 授权回调地址；换取用户信息时无需回调地址，传 null
     */
    public AuthRequest buildAuthRequest(SocialClientDO client, String redirectUri) {
        AuthConfig config = AuthConfig.builder().clientId(client.getClientId()).clientSecret(client.getClientSecret())
                .agentId(client.getAgentId()).redirectUri(redirectUri)
                .ignoreCheckRedirectUri(StrUtil.isEmpty(redirectUri)).build();
        SocialTypeEnum type = SocialTypeEnum.valueOf(client.getSocialType());
        if (type == null) {
            throw invalidParamException("未知的社交平台({})", client.getSocialType());
        }
        return switch (type) {
            case DINGTALK -> new AuthDingTalkRequest(config, authStateCache);
            case WECHAT_ENTERPRISE -> new AuthWeChatEnterpriseQrcodeRequest(config, authStateCache);
            case WECHAT_MP -> new AuthWeChatMpRequest(config, authStateCache);
            case WECHAT_OPEN -> new AuthWeChatOpenRequest(config, authStateCache);
            case WECHAT_MINI_PROGRAM -> throw invalidParamException("微信小程序不走网页授权，请使用小程序登录");
        };
    }

    /**
     * 小程序客户端，access_token 存 Redis：多实例共用一份，避免各自刷新导致彼此失效
     */
    public WxMaService buildWxMaService(SocialClientDO client) {
        String key = client.getClientId() + ":" + client.getClientSecret().hashCode();
        return wxMaServices.computeIfAbsent(key, k -> {
            WxMaRedisBetterConfigImpl config = new WxMaRedisBetterConfigImpl(
                    new RedisTemplateWxRedisOps(stringRedisTemplate), "ccloud:wx_ma");
            config.setAppid(client.getClientId());
            config.setSecret(client.getClientSecret());
            WxMaService service = new WxMaServiceImpl();
            service.setWxMaConfig(config);
            return service;
        });
    }
}

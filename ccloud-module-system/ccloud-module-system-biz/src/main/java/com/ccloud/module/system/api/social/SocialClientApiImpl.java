package com.ccloud.module.system.api.social;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import com.ccloud.module.system.service.social.SocialClientService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 社交客户端 RPC 实现：会员服务的社交登录、小程序一键登录依赖它
 */
@RestController
@Validated
public class SocialClientApiImpl implements SocialClientApi {

    @Resource
    private SocialClientService socialClientService;

    @Override
    public CommonResult<String> getAuthorizeUrl(Integer socialType, Integer userType, String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(socialType, userType, redirectUri));
    }

    @Override
    public CommonResult<SocialWxPhoneNumberInfoRespDTO> getWxMaPhoneNumberInfo(Integer userType, String phoneCode) {
        return success(socialClientService.getWxMaPhoneNumberInfo(userType, phoneCode));
    }
}

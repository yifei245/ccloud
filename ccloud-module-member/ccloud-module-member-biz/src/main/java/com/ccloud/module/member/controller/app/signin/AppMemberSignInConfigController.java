package com.ccloud.module.member.controller.app.signin;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.app.signin.vo.AppMemberSignInConfigRespVO;
import com.ccloud.module.member.service.signin.MemberSignInConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 签到规则")
@RestController
@RequestMapping("/app-api/member/sign-in/config")
@Validated
public class AppMemberSignInConfigController {

    @Resource
    private MemberSignInConfigService configService;

    @GetMapping("/list")
    @Operation(summary = "获得启用的签到规则", description = "按天数升序")
    public CommonResult<List<AppMemberSignInConfigRespVO>> getSignInConfigList() {
        return success(BeanUtils.toBean(configService.getSignInConfigList(CommonStatusEnum.ENABLE.getStatus()),
                AppMemberSignInConfigRespVO.class));
    }
}

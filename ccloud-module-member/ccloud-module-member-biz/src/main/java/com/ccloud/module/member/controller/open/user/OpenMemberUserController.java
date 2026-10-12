package com.ccloud.module.member.controller.open.user;

import cn.hutool.core.util.DesensitizedUtil;
import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.validation.Mobile;
import com.ccloud.module.member.controller.open.user.vo.OpenMemberUserRespVO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.service.level.MemberLevelService;
import com.ccloud.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_MOBILE_NOT_EXISTS;

@Tag(name = "开放接口 - 会员")
@RestController
@RequestMapping("/open-api/member/user")
@Validated
public class OpenMemberUserController {

    @Resource
    private MemberUserService memberUserService;
    @Resource
    private MemberLevelService memberLevelService;

    /**
     * 身份由网关验签后下发（开放应用），框架只在 /open-api 下接受开放应用身份；租户为应用所属租户
     */
    @GetMapping("/get")
    // 调用日志（含 open.api-log）不得留下被查询的手机号明文
    @ApiAccessLog(sanitizeKeys = "mobile")
    @Operation(summary = "按手机号查询会员基本信息", description = "手机号脱敏；只查应用所属租户的会员")
    @Parameter(name = "mobile", description = "手机号", required = true, example = "13800138000")
    public CommonResult<OpenMemberUserRespVO> getUser(@RequestParam("mobile") @Mobile String mobile) {
        MemberUserDO user = memberUserService.getUserByMobile(mobile);
        if (user == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }
        MemberLevelDO level = user.getLevelId() == null ? null : memberLevelService.getLevel(user.getLevelId());
        return success(new OpenMemberUserRespVO().setId(user.getId()).setNickname(user.getNickname())
                .setAvatar(user.getAvatar()).setMobile(DesensitizedUtil.mobilePhone(user.getMobile()))
                .setLevelName(level == null ? null : level.getName()).setPoint(user.getPoint())
                .setStatus(user.getStatus()));
    }
}

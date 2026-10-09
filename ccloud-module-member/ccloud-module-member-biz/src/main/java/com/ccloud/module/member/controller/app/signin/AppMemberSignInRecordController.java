package com.ccloud.module.member.controller.app.signin;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.member.controller.app.signin.vo.AppMemberSignInRecordRespVO;
import com.ccloud.module.member.controller.app.signin.vo.AppMemberSignInRecordSummaryRespVO;
import com.ccloud.module.member.service.signin.MemberSignInRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 签到")
@RestController
@RequestMapping("/app-api/member/sign-in/record")
@Validated
public class AppMemberSignInRecordController {

    @Resource
    private MemberSignInRecordService signInRecordService;

    @GetMapping("/get-summary")
    @Operation(summary = "获得我的签到概况", description = "今天是否已签到、当前连续天数、累计天数")
    public CommonResult<AppMemberSignInRecordSummaryRespVO> getSignInRecordSummary() {
        return success(signInRecordService.getSignInRecordSummary(SecurityFrameworkUtils.getLoginUserId()));
    }

    @PostMapping("/create")
    @Operation(summary = "签到", description = "返回本次签到的连续天数与获得的奖励；今天已签到时返回错误")
    public CommonResult<AppMemberSignInRecordRespVO> createSignInRecord() {
        return success(BeanUtils.toBean(signInRecordService.createSignRecord(SecurityFrameworkUtils.getLoginUserId()),
                AppMemberSignInRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得我的签到记录分页", description = "按签到时间倒序")
    public CommonResult<PageResult<AppMemberSignInRecordRespVO>> getSignInRecordPage(@Valid PageParam pageParam) {
        return success(BeanUtils.toBean(signInRecordService.getSignInRecordPage(SecurityFrameworkUtils.getLoginUserId(), pageParam),
                AppMemberSignInRecordRespVO.class));
    }
}

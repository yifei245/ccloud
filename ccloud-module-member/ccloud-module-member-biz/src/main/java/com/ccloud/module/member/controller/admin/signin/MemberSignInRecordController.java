package com.ccloud.module.member.controller.admin.signin;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInRecordPageReqVO;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInRecordRespVO;
import com.ccloud.module.member.service.signin.MemberSignInRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 签到记录")
@RestController
@RequestMapping("/admin-api/member/sign-in/record")
@Validated
public class MemberSignInRecordController {

    @Resource
    private MemberSignInRecordService signInRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得签到记录分页")
    @PreAuthorize("@ss.hasPermission('member:sign-in-record:query')")
    public CommonResult<PageResult<MemberSignInRecordRespVO>> getSignInRecordPage(@Valid MemberSignInRecordPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(signInRecordService.getSignInRecordPage(pageReqVO), MemberSignInRecordRespVO.class));
    }
}

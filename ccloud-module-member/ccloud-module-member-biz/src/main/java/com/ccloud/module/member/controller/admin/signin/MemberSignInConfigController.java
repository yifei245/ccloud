package com.ccloud.module.member.controller.admin.signin;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInConfigRespVO;
import com.ccloud.module.member.controller.admin.signin.vo.MemberSignInConfigSaveReqVO;
import com.ccloud.module.member.service.signin.MemberSignInConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 签到规则")
@RestController
@RequestMapping("/admin-api/member/sign-in/config")
@Validated
public class MemberSignInConfigController {

    @Resource
    private MemberSignInConfigService configService;

    @PostMapping("/create")
    @Operation(summary = "创建签到规则")
    @PreAuthorize("@ss.hasPermission('member:sign-in-config:create')")
    public CommonResult<Long> createSignInConfig(@Valid @RequestBody MemberSignInConfigSaveReqVO createReqVO) {
        return success(configService.createSignInConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改签到规则")
    @PreAuthorize("@ss.hasPermission('member:sign-in-config:update')")
    public CommonResult<Boolean> updateSignInConfig(@Valid @RequestBody MemberSignInConfigSaveReqVO updateReqVO) {
        configService.updateSignInConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除签到规则")
    @Parameter(name = "id", description = "规则编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:sign-in-config:delete')")
    public CommonResult<Boolean> deleteSignInConfig(@RequestParam("id") Long id) {
        configService.deleteSignInConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得签到规则")
    @Parameter(name = "id", description = "规则编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:sign-in-config:query')")
    public CommonResult<MemberSignInConfigRespVO> getSignInConfig(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(configService.getSignInConfig(id), MemberSignInConfigRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得全部签到规则", description = "含停用的规则，按天数升序")
    @PreAuthorize("@ss.hasPermission('member:sign-in-config:query')")
    public CommonResult<List<MemberSignInConfigRespVO>> getSignInConfigList() {
        return success(BeanUtils.toBean(configService.getSignInConfigList(null), MemberSignInConfigRespVO.class));
    }
}

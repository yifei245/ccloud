package com.ccloud.module.member.controller.admin.config;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.config.vo.MemberConfigRespVO;
import com.ccloud.module.member.controller.admin.config.vo.MemberConfigSaveReqVO;
import com.ccloud.module.member.service.config.MemberConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 会员配置")
@RestController
@RequestMapping("/admin-api/member/config")
@Validated
public class MemberConfigController {

    @Resource
    private MemberConfigService configService;

    @PutMapping("/save")
    @Operation(summary = "保存会员配置", description = "当前租户没有配置时新增，已有时修改")
    @PreAuthorize("@ss.hasPermission('member:config:update')")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody MemberConfigSaveReqVO saveReqVO) {
        configService.saveConfig(saveReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员配置", description = "未配置时返回 null，由前端按默认值展示")
    @PreAuthorize("@ss.hasPermission('member:config:query')")
    public CommonResult<MemberConfigRespVO> getConfig() {
        return success(BeanUtils.toBean(configService.getConfig(), MemberConfigRespVO.class));
    }
}

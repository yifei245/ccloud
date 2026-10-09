package com.ccloud.module.member.controller.admin.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.level.vo.MemberExperienceRecordPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberExperienceRecordRespVO;
import com.ccloud.module.member.service.level.MemberLevelService;
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

@Tag(name = "管理后台 - 会员经验记录")
@RestController
@RequestMapping("/admin-api/member/experience-record")
@Validated
public class MemberExperienceRecordController {

    @Resource
    private MemberLevelService levelService;

    @GetMapping("/page")
    @Operation(summary = "获得会员经验记录分页")
    @PreAuthorize("@ss.hasPermission('member:experience-record:query')")
    public CommonResult<PageResult<MemberExperienceRecordRespVO>> getExperienceRecordPage(
            @Valid MemberExperienceRecordPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(levelService.getExperienceRecordPage(pageReqVO), MemberExperienceRecordRespVO.class));
    }
}

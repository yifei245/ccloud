package com.ccloud.module.member.controller.admin.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelRecordPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelRecordRespVO;
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

@Tag(name = "管理后台 - 会员等级变更记录")
@RestController
@RequestMapping("/admin-api/member/level-record")
@Validated
public class MemberLevelRecordController {

    @Resource
    private MemberLevelService levelService;

    @GetMapping("/page")
    @Operation(summary = "获得会员等级变更记录分页")
    @PreAuthorize("@ss.hasPermission('member:level-record:query')")
    public CommonResult<PageResult<MemberLevelRecordRespVO>> getLevelRecordPage(@Valid MemberLevelRecordPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(levelService.getLevelRecordPage(pageReqVO), MemberLevelRecordRespVO.class));
    }
}

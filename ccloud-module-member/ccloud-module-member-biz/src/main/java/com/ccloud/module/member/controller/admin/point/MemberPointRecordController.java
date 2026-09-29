package com.ccloud.module.member.controller.admin.point;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.point.vo.MemberPointRecordPageReqVO;
import com.ccloud.module.member.controller.admin.point.vo.MemberPointRecordRespVO;
import com.ccloud.module.member.service.point.MemberPointService;
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

@Tag(name = "管理后台 - 会员积分记录")
@RestController
@RequestMapping("/admin-api/member/point/record")
@Validated
public class MemberPointRecordController {

    @Resource
    private MemberPointService pointService;

    @GetMapping("/page")
    @Operation(summary = "获得会员积分记录分页")
    @PreAuthorize("@ss.hasPermission('member:point-record:query')")
    public CommonResult<PageResult<MemberPointRecordRespVO>> getPointRecordPage(@Valid MemberPointRecordPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(pointService.getPointRecordPage(pageReqVO), MemberPointRecordRespVO.class));
    }
}

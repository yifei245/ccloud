package com.ccloud.module.member.controller.app.point;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import com.ccloud.module.member.controller.app.point.vo.AppMemberPointRecordRespVO;
import com.ccloud.module.member.service.point.MemberPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 积分记录")
@RestController
@RequestMapping("/app-api/member/point/record")
@Validated
public class AppMemberPointRecordController {

    @Resource
    private MemberPointService pointService;

    @GetMapping("/page")
    @Operation(summary = "获得我的积分记录分页", description = "按时间倒序，可按收入或支出筛选")
    public CommonResult<PageResult<AppMemberPointRecordRespVO>> getPointRecordPage(@Valid AppMemberPointRecordPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(pointService.getPointRecordPage(SecurityFrameworkUtils.getLoginUserId(), pageReqVO),
                AppMemberPointRecordRespVO.class));
    }
}

package com.ccloud.module.member.controller.app.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.member.controller.app.level.vo.AppMemberExperienceRecordRespVO;
import com.ccloud.module.member.service.level.MemberLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 经验记录")
@RestController
@RequestMapping("/app-api/member/experience-record")
@Validated
public class AppMemberExperienceRecordController {

    @Resource
    private MemberLevelService levelService;

    @GetMapping("/page")
    @Operation(summary = "获得我的经验记录分页", description = "按时间倒序")
    public CommonResult<PageResult<AppMemberExperienceRecordRespVO>> getExperienceRecordPage(@Valid PageParam pageParam) {
        return success(BeanUtils.toBean(levelService.getExperienceRecordPage(SecurityFrameworkUtils.getLoginUserId(), pageParam),
                AppMemberExperienceRecordRespVO.class));
    }
}

package com.ccloud.module.member.api.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.service.level.MemberLevelService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class MemberExperienceApiImpl implements MemberExperienceApi {

    @Resource
    private MemberLevelService levelService;

    @Override
    public CommonResult<Boolean> addExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        levelService.addExperience(userId, experience, bizType, bizId);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> reduceExperience(Long userId, Integer experience, Integer bizType, String bizId) {
        levelService.reduceExperience(userId, experience, bizType, bizId);
        return success(true);
    }
}

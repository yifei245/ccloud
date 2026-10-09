package com.ccloud.module.member.api.point;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.service.point.MemberPointService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class MemberPointApiImpl implements MemberPointApi {

    @Resource
    private MemberPointService pointService;

    @Override
    public CommonResult<Boolean> addPoint(Long userId, Integer point, Integer bizType, String bizId) {
        pointService.addPoint(userId, point, bizType, bizId);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> reducePoint(Long userId, Integer point, Integer bizType, String bizId) {
        pointService.reducePoint(userId, point, bizType, bizId);
        return success(true);
    }
}

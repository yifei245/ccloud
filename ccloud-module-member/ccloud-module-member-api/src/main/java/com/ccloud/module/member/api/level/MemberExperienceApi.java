package com.ccloud.module.member.api.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 经验变动会同步重算会员等级
 */
@FeignClient(name = ApiConstants.NAME, contextId = "memberMemberExperienceApi")
@Tag(name = "RPC 服务 - 会员经验")
public interface MemberExperienceApi {

    String PREFIX = ApiConstants.PREFIX + "/experience";

    @PutMapping(PREFIX + "/add")
    @Operation(summary = "增加经验", description = "bizType 见 MemberExperienceBizTypeEnum；同一业务编号重复调用只生效一次")
    CommonResult<Boolean> addExperience(@RequestParam("userId") Long userId,
                                        @RequestParam("experience") Integer experience,
                                        @RequestParam("bizType") Integer bizType,
                                        @RequestParam("bizId") String bizId);

    @PutMapping(PREFIX + "/reduce")
    @Operation(summary = "扣减经验", description = "bizType 见 MemberExperienceBizTypeEnum；同一业务编号重复调用只生效一次")
    CommonResult<Boolean> reduceExperience(@RequestParam("userId") Long userId,
                                           @RequestParam("experience") Integer experience,
                                           @RequestParam("bizType") Integer bizType,
                                           @RequestParam("bizId") String bizId);
}

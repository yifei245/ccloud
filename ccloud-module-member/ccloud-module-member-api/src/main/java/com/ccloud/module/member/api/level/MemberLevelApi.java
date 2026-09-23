package com.ccloud.module.member.api.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.api.level.dto.MemberLevelRespDTO;
import com.ccloud.module.member.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "memberMemberLevelApi")
@Tag(name = "RPC 服务 - 会员等级")
public interface MemberLevelApi {

    String PREFIX = ApiConstants.PREFIX + "/level";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得会员等级")
    CommonResult<MemberLevelRespDTO> getMemberLevel(@RequestParam("id") Long id);
}

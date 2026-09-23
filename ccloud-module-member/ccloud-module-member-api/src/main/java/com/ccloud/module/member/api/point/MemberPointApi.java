package com.ccloud.module.member.api.point;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME, contextId = "memberMemberPointApi")
@Tag(name = "RPC 服务 - 会员积分")
public interface MemberPointApi {

    String PREFIX = ApiConstants.PREFIX + "/point";

    @PutMapping(PREFIX + "/add")
    @Operation(summary = "增加积分", description = "bizType 见 MemberPointBizTypeEnum；同一业务编号重复调用只生效一次")
    CommonResult<Boolean> addPoint(@RequestParam("userId") Long userId,
                                   @RequestParam("point") Integer point,
                                   @RequestParam("bizType") Integer bizType,
                                   @RequestParam("bizId") String bizId);

    @PutMapping(PREFIX + "/reduce")
    @Operation(summary = "扣减积分", description = "余额不足时返回 USER_POINT_NOT_ENOUGH；同一业务编号重复调用只生效一次")
    CommonResult<Boolean> reducePoint(@RequestParam("userId") Long userId,
                                      @RequestParam("point") Integer point,
                                      @RequestParam("bizType") Integer bizType,
                                      @RequestParam("bizId") String bizId);
}

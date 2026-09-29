package com.ccloud.module.member.api.user;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.member.api.user.dto.MemberUserRespDTO;
import com.ccloud.module.member.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(name = ApiConstants.NAME, contextId = "memberMemberUserApi")
@Tag(name = "RPC 服务 - 会员用户")
public interface MemberUserApi {

    String PREFIX = ApiConstants.PREFIX + "/user";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得会员用户信息")
    CommonResult<MemberUserRespDTO> getUser(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得会员用户信息列表")
    CommonResult<List<MemberUserRespDTO>> getUserList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/list-by-nickname")
    @Operation(summary = "按昵称模糊查询会员用户")
    CommonResult<List<MemberUserRespDTO>> getUserListByNickname(@RequestParam("nickname") String nickname);

    @GetMapping(PREFIX + "/get-by-mobile")
    @Operation(summary = "按手机号获得会员用户，未注册返回 null")
    CommonResult<MemberUserRespDTO> getUserByMobile(@RequestParam("mobile") String mobile);

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验会员用户存在，不存在时返回 USER_NOT_EXISTS")
    CommonResult<Boolean> validateUser(@RequestParam("id") Long id);
}

package com.ccloud.module.member.controller.app.address;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressCreateReqVO;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressRespVO;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.address.MemberAddressDO;
import com.ccloud.module.member.service.address.MemberAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.ccloud.module.member.enums.ErrorCodeConstants.ADDRESS_NOT_EXISTS;

/**
 * 只能操作当前登录用户本人的地址，用户编号一律取自登录身份
 */
@Tag(name = "用户 App - 收件地址")
@RestController
@RequestMapping("/app-api/member/address")
@Validated
public class AppMemberAddressController {

    @Resource
    private MemberAddressService addressService;

    @PostMapping("/create")
    @Operation(summary = "创建收件地址", description = "第一个地址自动设为默认；每人最多 20 个")
    public CommonResult<Long> createAddress(@Valid @RequestBody AppMemberAddressCreateReqVO createReqVO) {
        return success(addressService.createAddress(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改收件地址")
    public CommonResult<Boolean> updateAddress(@Valid @RequestBody AppMemberAddressUpdateReqVO updateReqVO) {
        addressService.updateAddress(getLoginUserId(), updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收件地址", description = "删除默认地址后不会自动另选")
    @Parameter(name = "id", description = "地址编号", required = true, example = "1")
    public CommonResult<Boolean> deleteAddress(@RequestParam("id") Long id) {
        addressService.deleteAddress(getLoginUserId(), id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收件地址")
    @Parameter(name = "id", description = "地址编号", required = true, example = "1")
    public CommonResult<AppMemberAddressRespVO> getAddress(@RequestParam("id") Long id) {
        MemberAddressDO address = addressService.getAddress(getLoginUserId(), id);
        if (address == null) {
            throw exception(ADDRESS_NOT_EXISTS);
        }
        return success(BeanUtils.toBean(address, AppMemberAddressRespVO.class));
    }

    @GetMapping("/get-default")
    @Operation(summary = "获得默认收件地址", description = "未设置时返回 null")
    public CommonResult<AppMemberAddressRespVO> getDefaultAddress() {
        return success(BeanUtils.toBean(addressService.getDefaultAddress(getLoginUserId()), AppMemberAddressRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得收件地址列表", description = "默认地址在前")
    public CommonResult<List<AppMemberAddressRespVO>> getAddressList() {
        return success(BeanUtils.toBean(addressService.getAddressList(getLoginUserId()), AppMemberAddressRespVO.class));
    }
}

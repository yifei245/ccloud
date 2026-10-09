package com.ccloud.module.member.api.address;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.api.address.dto.MemberAddressRespDTO;
import com.ccloud.module.member.service.address.MemberAddressService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class MemberAddressApiImpl implements MemberAddressApi {

    @Resource
    private MemberAddressService addressService;

    @Override
    public CommonResult<MemberAddressRespDTO> getAddress(Long id, Long userId) {
        return success(BeanUtils.toBean(addressService.getAddress(userId, id), MemberAddressRespDTO.class));
    }

    @Override
    public CommonResult<MemberAddressRespDTO> getDefaultAddress(Long userId) {
        return success(BeanUtils.toBean(addressService.getDefaultAddress(userId), MemberAddressRespDTO.class));
    }
}

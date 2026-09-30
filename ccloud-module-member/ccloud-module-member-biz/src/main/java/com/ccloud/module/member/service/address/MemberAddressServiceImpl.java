package com.ccloud.module.member.service.address;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressCreateReqVO;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.address.MemberAddressDO;
import com.ccloud.module.member.dal.mysql.address.MemberAddressMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.member.enums.ErrorCodeConstants.ADDRESS_COUNT_EXCEEDED;
import static com.ccloud.module.member.enums.ErrorCodeConstants.ADDRESS_NOT_EXISTS;

@Service
@Validated
public class MemberAddressServiceImpl implements MemberAddressService {

    /**
     * 每个会员的地址数量上限
     */
    public static final int MAX_COUNT = 20;

    @Resource
    private MemberAddressMapper addressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAddress(Long userId, AppMemberAddressCreateReqVO reqVO) {
        long count = addressMapper.selectCount(MemberAddressDO::getUserId, userId);
        if (count >= MAX_COUNT) {
            throw exception(ADDRESS_COUNT_EXCEEDED, MAX_COUNT);
        }
        // 第一个地址无论请求如何都设为默认，下单时总能取到默认地址
        boolean isDefault = count == 0 || Boolean.TRUE.equals(reqVO.getDefaultStatus());
        if (isDefault) {
            addressMapper.updateDefaultFalseByUserId(userId);
        }
        MemberAddressDO address = BeanUtils.toBean(reqVO, MemberAddressDO.class)
                .setUserId(userId).setDefaultStatus(isDefault);
        addressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, AppMemberAddressUpdateReqVO reqVO) {
        validateAddressExists(userId, reqVO.getId());
        if (Boolean.TRUE.equals(reqVO.getDefaultStatus())) {
            addressMapper.updateDefaultFalseByUserId(userId);
        }
        addressMapper.updateById(BeanUtils.toBean(reqVO, MemberAddressDO.class).setUserId(userId));
    }

    @Override
    public void deleteAddress(Long userId, Long id) {
        validateAddressExists(userId, id);
        addressMapper.deleteById(id);
    }

    private void validateAddressExists(Long userId, Long id) {
        if (getAddress(userId, id) == null) {
            throw exception(ADDRESS_NOT_EXISTS);
        }
    }

    @Override
    public MemberAddressDO getAddress(Long userId, Long id) {
        return addressMapper.selectByIdAndUserId(id, userId);
    }

    @Override
    public MemberAddressDO getDefaultAddress(Long userId) {
        return addressMapper.selectDefaultByUserId(userId);
    }

    @Override
    public List<MemberAddressDO> getAddressList(Long userId) {
        return addressMapper.selectListByUserId(userId);
    }
}

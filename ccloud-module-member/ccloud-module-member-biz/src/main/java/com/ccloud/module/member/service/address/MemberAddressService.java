package com.ccloud.module.member.service.address;

import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressCreateReqVO;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.address.MemberAddressDO;

import java.util.List;

/**
 * 会员收件地址。所有方法都按用户过滤，访问他人地址视为不存在
 */
public interface MemberAddressService {

    /**
     * 第一个地址自动设为默认；设为默认时取消该用户原来的默认地址
     *
     * @throws com.ccloud.framework.common.exception.ServiceException 超过数量上限时抛 ADDRESS_COUNT_EXCEEDED
     */
    Long createAddress(Long userId, AppMemberAddressCreateReqVO reqVO);

    void updateAddress(Long userId, AppMemberAddressUpdateReqVO reqVO);

    /**
     * 删除默认地址后不自动另选
     */
    void deleteAddress(Long userId, Long id);

    /**
     * @return 不属于该用户时返回 null
     */
    MemberAddressDO getAddress(Long userId, Long id);

    /**
     * @return 未设置默认地址时返回 null
     */
    MemberAddressDO getDefaultAddress(Long userId);

    /**
     * @return 默认地址在前，其余按编号倒序
     */
    List<MemberAddressDO> getAddressList(Long userId);
}

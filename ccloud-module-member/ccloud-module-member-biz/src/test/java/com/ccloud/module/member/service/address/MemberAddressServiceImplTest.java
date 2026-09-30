package com.ccloud.module.member.service.address;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressCreateReqVO;
import com.ccloud.module.member.controller.app.address.vo.AppMemberAddressUpdateReqVO;
import com.ccloud.module.member.dal.dataobject.address.MemberAddressDO;
import com.ccloud.module.member.dal.mysql.address.MemberAddressMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.member.enums.ErrorCodeConstants.ADDRESS_COUNT_EXCEEDED;
import static com.ccloud.module.member.enums.ErrorCodeConstants.ADDRESS_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberAddressServiceImpl.class)
class MemberAddressServiceImplTest extends BaseMemberDbUnitTest {

    private static final Long USER = 1L;
    private static final Long OTHER = 2L;

    @Resource
    private MemberAddressServiceImpl addressService;
    @Resource
    private MemberAddressMapper addressMapper;

    @Test
    void 第一个地址自动设为默认_即使请求为非默认() {
        Long id = addressService.createAddress(USER, reqVO(false));
        MemberAddressDO address = addressMapper.selectById(id);
        assertThat(address.getDefaultStatus()).isTrue();
        assertThat(address.getUserId()).isEqualTo(USER);
        assertThat(address.getAreaId()).isEqualTo(440305L);
    }

    @Test
    void 已有地址时按请求决定是否默认() {
        Long first = addressService.createAddress(USER, reqVO(true));
        Long second = addressService.createAddress(USER, reqVO(false));
        assertThat(addressMapper.selectById(first).getDefaultStatus()).isTrue();
        assertThat(addressMapper.selectById(second).getDefaultStatus()).isFalse();
    }

    @Test
    void 设新的默认地址时取消原默认_不影响其他用户() {
        Long first = addressService.createAddress(USER, reqVO(true));
        Long others = addressService.createAddress(OTHER, reqVO(true));
        Long second = addressService.createAddress(USER, reqVO(true));

        assertThat(addressMapper.selectById(first).getDefaultStatus()).isFalse();
        assertThat(addressService.getDefaultAddress(USER).getId()).isEqualTo(second);
        assertThat(addressMapper.selectById(others).getDefaultStatus()).isTrue();
    }

    @Test
    void 修改为默认时同样取消原默认() {
        Long first = addressService.createAddress(USER, reqVO(true));
        Long second = addressService.createAddress(USER, reqVO(false));

        addressService.updateAddress(USER, updateReqVO(second, true));

        assertThat(addressMapper.selectById(first).getDefaultStatus()).isFalse();
        assertThat(addressMapper.selectById(second).getDefaultStatus()).isTrue();
        assertThat(addressMapper.selectById(second).getDetailAddress()).isEqualTo("科技园 2 号");
    }

    @Test
    void 删除默认地址后不自动另选() {
        Long first = addressService.createAddress(USER, reqVO(true));
        Long second = addressService.createAddress(USER, reqVO(false));

        addressService.deleteAddress(USER, first);

        assertThat(addressMapper.selectById(first)).isNull();
        assertThat(addressService.getDefaultAddress(USER)).isNull();
        assertThat(addressMapper.selectById(second).getDefaultStatus()).isFalse();
    }

    @Test
    void 超过20个时拒绝() {
        for (int i = 0; i < MemberAddressServiceImpl.MAX_COUNT; i++) {
            addressService.createAddress(USER, reqVO(false));
        }
        assertServiceException(() -> addressService.createAddress(USER, reqVO(false)), ADDRESS_COUNT_EXCEEDED);
        // 数量按用户计，其他用户不受影响
        addressService.createAddress(OTHER, reqVO(false));
    }

    @Test
    void 不能访问他人地址() {
        Long id = addressService.createAddress(USER, reqVO(true));

        assertThat(addressService.getAddress(OTHER, id)).isNull();
        assertServiceException(() -> addressService.updateAddress(OTHER, updateReqVO(id, true)), ADDRESS_NOT_EXISTS);
        assertServiceException(() -> addressService.deleteAddress(OTHER, id), ADDRESS_NOT_EXISTS);
        assertThat(addressMapper.selectById(id).getDetailAddress()).isEqualTo("科技园 1 号");
    }

    @Test
    void 列表只含本人地址_默认地址在前() {
        Long first = addressService.createAddress(USER, reqVO(false));
        Long second = addressService.createAddress(USER, reqVO(false));
        Long third = addressService.createAddress(USER, reqVO(false));
        addressService.createAddress(OTHER, reqVO(false));

        assertThat(addressService.getAddressList(USER)).extracting(MemberAddressDO::getId)
                .containsExactly(first, third, second);
    }

    private static AppMemberAddressCreateReqVO reqVO(boolean defaultStatus) {
        AppMemberAddressCreateReqVO reqVO = new AppMemberAddressCreateReqVO();
        reqVO.setName("张三");
        reqVO.setMobile("13800138000");
        reqVO.setAreaId(440305L);
        reqVO.setDetailAddress("科技园 1 号");
        reqVO.setDefaultStatus(defaultStatus);
        return reqVO;
    }

    private static AppMemberAddressUpdateReqVO updateReqVO(Long id, boolean defaultStatus) {
        AppMemberAddressUpdateReqVO reqVO = BeanUtils.toBean(reqVO(defaultStatus), AppMemberAddressUpdateReqVO.class);
        reqVO.setId(id);
        reqVO.setDetailAddress("科技园 2 号");
        return reqVO;
    }
}

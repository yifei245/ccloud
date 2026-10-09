package com.ccloud.module.member.service.group;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupPageReqVO;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupSaveReqVO;
import com.ccloud.module.member.dal.dataobject.group.MemberGroupDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.group.MemberGroupMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.member.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberGroupServiceImpl.class)
class MemberGroupServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberGroupServiceImpl groupService;
    @Resource
    private MemberGroupMapper groupMapper;
    @Resource
    private MemberUserMapper userMapper;

    @Test
    void createGroup_成功与名称重复() {
        Long id = groupService.createGroup(save(null, "普通", CommonStatusEnum.ENABLE));
        MemberGroupDO group = groupMapper.selectById(id);
        assertThat(group.getName()).isEqualTo("普通");
        assertThat(group.getRemark()).isEqualTo("备注");
        assertServiceException(() -> groupService.createGroup(save(null, "普通", CommonStatusEnum.ENABLE)),
                GROUP_NAME_EXISTS);
    }

    @Test
    void updateGroup_保持自身名称不算重复_改成他人名称报错_不存在报错() {
        Long a = groupService.createGroup(save(null, "A", CommonStatusEnum.ENABLE));
        groupService.createGroup(save(null, "B", CommonStatusEnum.ENABLE));
        groupService.updateGroup(save(a, "A", CommonStatusEnum.DISABLE));
        assertThat(groupMapper.selectById(a).getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());
        assertServiceException(() -> groupService.updateGroup(save(a, "B", CommonStatusEnum.ENABLE)), GROUP_NAME_EXISTS);
        assertServiceException(() -> groupService.updateGroup(save(-1L, "C", CommonStatusEnum.ENABLE)), GROUP_NOT_EXISTS);
    }

    @Test
    void deleteGroup_有会员在用时拒绝_否则删除() {
        Long used = groupService.createGroup(save(null, "在用", CommonStatusEnum.ENABLE));
        Long unused = groupService.createGroup(save(null, "空闲", CommonStatusEnum.ENABLE));
        userMapper.insert(new MemberUserDO().setMobile("13900000001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(0).setExperience(0)
                .setGroupId(used));

        assertServiceException(() -> groupService.deleteGroup(used), GROUP_HAS_USER);
        groupService.deleteGroup(unused);
        assertThat(groupMapper.selectById(unused)).isNull();
        assertServiceException(() -> groupService.deleteGroup(unused), GROUP_NOT_EXISTS);
    }

    @Test
    void validateGroupEnabled() {
        Long enabled = groupService.createGroup(save(null, "普通", CommonStatusEnum.ENABLE));
        Long disabled = groupService.createGroup(save(null, "冻结", CommonStatusEnum.DISABLE));
        groupService.validateGroupEnabled(enabled);
        assertServiceException(() -> groupService.validateGroupEnabled(disabled), GROUP_DISABLED);
        assertServiceException(() -> groupService.validateGroupEnabled(-1L), GROUP_NOT_EXISTS);
    }

    @Test
    void 分页与启用列表() {
        groupService.createGroup(save(null, "普通会员", CommonStatusEnum.ENABLE));
        groupService.createGroup(save(null, "冻结会员", CommonStatusEnum.DISABLE));
        groupService.createGroup(save(null, "内部", CommonStatusEnum.ENABLE));

        MemberGroupPageReqVO reqVO = new MemberGroupPageReqVO();
        reqVO.setName("会员");
        assertThat(groupService.getGroupPage(reqVO).getTotal()).isEqualTo(2);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        assertThat(groupService.getGroupPage(reqVO).getList()).extracting(MemberGroupDO::getName)
                .containsExactly("冻结会员");
        assertThat(groupService.getEnableGroupList()).extracting(MemberGroupDO::getName)
                .containsExactlyInAnyOrder("普通会员", "内部");
    }

    private static MemberGroupSaveReqVO save(Long id, String name, CommonStatusEnum status) {
        MemberGroupSaveReqVO reqVO = new MemberGroupSaveReqVO();
        reqVO.setId(id);
        reqVO.setName(name);
        reqVO.setRemark("备注");
        reqVO.setStatus(status.getStatus());
        return reqVO;
    }
}

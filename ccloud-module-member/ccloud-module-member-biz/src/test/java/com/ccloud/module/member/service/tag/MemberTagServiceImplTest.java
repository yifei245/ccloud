package com.ccloud.module.member.service.tag;

import com.ccloud.module.member.controller.admin.tag.vo.MemberTagPageReqVO;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagSaveReqVO;
import com.ccloud.module.member.dal.dataobject.tag.MemberTagDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.tag.MemberTagMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.member.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberTagServiceImpl.class)
class MemberTagServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberTagServiceImpl tagService;
    @Resource
    private MemberTagMapper tagMapper;
    @Resource
    private MemberUserMapper userMapper;

    @Test
    void createTag_成功与名称重复() {
        Long id = tagService.createTag(save(null, "高价值"));
        assertThat(tagMapper.selectById(id).getName()).isEqualTo("高价值");
        assertServiceException(() -> tagService.createTag(save(null, "高价值")), TAG_NAME_EXISTS);
    }

    @Test
    void updateTag_保持自身名称不算重复_改成他人名称报错_不存在报错() {
        Long a = tagService.createTag(save(null, "A"));
        tagService.createTag(save(null, "B"));
        tagService.updateTag(save(a, "A"));
        assertServiceException(() -> tagService.updateTag(save(a, "B")), TAG_NAME_EXISTS);
        assertServiceException(() -> tagService.updateTag(save(-1L, "C")), TAG_NOT_EXISTS);
    }

    @Test
    void deleteTag_有会员在用时拒绝_逗号列表中的任一位置都能识别() {
        Long a = tagService.createTag(save(null, "A"));
        Long b = tagService.createTag(save(null, "B"));
        insertUserWithTags(a + "," + b);
        assertServiceException(() -> tagService.deleteTag(b), TAG_HAS_USER);
        assertServiceException(() -> tagService.deleteTag(a), TAG_HAS_USER);
        assertThat(tagMapper.selectById(a)).isNotNull();
    }

    @Test
    void deleteTag_编号是其他编号的子串时不误判() {
        Long a = tagService.createTag(save(null, "A"));
        insertUserWithTags(a + "0," + "9" + a);
        tagService.deleteTag(a);
        assertThat(tagMapper.selectById(a)).isNull();
        assertServiceException(() -> tagService.deleteTag(a), TAG_NOT_EXISTS);
    }

    @Test
    void validateTagList_任一不存在即报错_空集合通过() {
        Long a = tagService.createTag(save(null, "A"));
        Long b = tagService.createTag(save(null, "B"));
        tagService.validateTagList(List.of());
        tagService.validateTagList(List.of(a, b));
        assertServiceException(() -> tagService.validateTagList(List.of(a, -1L)), TAG_NOT_EXISTS);
        assertServiceException(() -> tagService.validateTagList(List.of(-1L, a)), TAG_NOT_EXISTS);
    }

    @Test
    void getTagPage_按名称模糊查询() {
        tagService.createTag(save(null, "高价值"));
        tagService.createTag(save(null, "价值一般"));
        tagService.createTag(save(null, "新客"));
        MemberTagPageReqVO reqVO = new MemberTagPageReqVO();
        reqVO.setName("价值");
        assertThat(tagService.getTagPage(reqVO).getList()).extracting(MemberTagDO::getName)
                .containsExactlyInAnyOrder("高价值", "价值一般");
    }

    private static MemberTagSaveReqVO save(Long id, String name) {
        MemberTagSaveReqVO reqVO = new MemberTagSaveReqVO();
        reqVO.setId(id);
        reqVO.setName(name);
        return reqVO;
    }

    private void insertUserWithTags(String tagIds) {
        userMapper.insert(new MemberUserDO().setMobile("13900000001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(0).setExperience(0)
                .setTagIds(tagIds));
    }
}

package com.ccloud.module.system.service.dept;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.PostDO;
import com.ccloud.module.system.dal.mysql.dept.PostMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(PostServiceImpl.class)
class PostServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private PostService postService;
    @Resource
    private PostMapper postMapper;

    @Test
    void createPost_成功() {
        Long id = postService.createPost(newReqVO("ceo", "董事长"));
        PostDO post = postMapper.selectById(id);
        assertThat(post.getCode()).isEqualTo("ceo");
        assertThat(post.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createPost_编码重复() {
        postService.createPost(newReqVO("ceo", "董事长"));
        assertServiceException(() -> postService.createPost(newReqVO("ceo", "总经理")), POST_CODE_DUPLICATE);
    }

    @Test
    void createPost_名称重复() {
        postService.createPost(newReqVO("ceo", "董事长"));
        assertServiceException(() -> postService.createPost(newReqVO("chairman", "董事长")), POST_NAME_DUPLICATE);
    }

    @Test
    void updatePost_成功_与自身相同的编码名称允许() {
        Long id = postService.createPost(newReqVO("hr", "人事"));
        PostSaveReqVO reqVO = newReqVO("hr", "人力资源");
        reqVO.setId(id);
        postService.updatePost(reqVO);
        assertThat(postMapper.selectById(id).getName()).isEqualTo("人力资源");
    }

    @Test
    void updatePost_不存在() {
        PostSaveReqVO reqVO = newReqVO("x", "x");
        reqVO.setId(-1L);
        assertServiceException(() -> postService.updatePost(reqVO), POST_NOT_FOUND);
    }

    @Test
    void updatePost_编码与其他岗位重复() {
        postService.createPost(newReqVO("taken", "占用"));
        Long id = postService.createPost(newReqVO("self", "自身"));
        PostSaveReqVO reqVO = newReqVO("taken", "自身");
        reqVO.setId(id);
        assertServiceException(() -> postService.updatePost(reqVO), POST_CODE_DUPLICATE);
    }

    @Test
    void deletePost_成功与不存在() {
        Long id = postService.createPost(newReqVO("del", "待删"));
        postService.deletePost(id);
        assertThat(postMapper.selectById(id)).isNull();
        assertServiceException(() -> postService.deletePost(-1L), POST_NOT_FOUND);
    }

    @Test
    void deletePostList() {
        Long a = postService.createPost(newReqVO("a", "甲"));
        Long b = postService.createPost(newReqVO("b", "乙"));
        postService.deletePostList(List.of(a, b));
        assertThat(postMapper.selectCount()).isZero();
    }

    @Test
    void getPostPage_按条件过滤() {
        postService.createPost(newReqVO("dev_java", "Java 开发"));
        Long disabled = postService.createPost(newReqVO("dev_go", "Go 开发"));
        postMapper.updateById(new PostDO().setId(disabled).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        postService.createPost(newReqVO("qa", "测试"));

        PostPageReqVO reqVO = new PostPageReqVO();
        reqVO.setCode("dev");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<PostDO> page = postService.getPostPage(reqVO);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(PostDO::getCode).containsExactly("dev_java");
    }

    @Test
    void getPostList_按编号与状态() {
        Long a = postService.createPost(newReqVO("a", "甲"));
        Long b = postService.createPost(newReqVO("b", "乙"));
        postMapper.updateById(new PostDO().setId(b).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        assertThat(postService.getPostList(List.of(a, b))).hasSize(2);
        assertThat(postService.getPostList(null, Set.of(CommonStatusEnum.ENABLE.getStatus())))
                .extracting(PostDO::getId).containsExactly(a);
        assertThat(postService.getPost(a).getName()).isEqualTo("甲");
    }

    @Test
    void validatePostList_三个分支() {
        Long ok = postService.createPost(newReqVO("ok", "可用"));
        Long disabled = postService.createPost(newReqVO("off", "停用"));
        postMapper.updateById(new PostDO().setId(disabled).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        postService.validatePostList(List.of(ok));
        postService.validatePostList(null);
        assertServiceException(() -> postService.validatePostList(List.of(-1L)), POST_NOT_FOUND);
        assertServiceException(() -> postService.validatePostList(List.of(disabled)), POST_NOT_ENABLE);
    }

    private static PostSaveReqVO newReqVO(String code, String name) {
        PostSaveReqVO reqVO = new PostSaveReqVO();
        reqVO.setCode(code);
        reqVO.setName(name);
        reqVO.setSort(1);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}

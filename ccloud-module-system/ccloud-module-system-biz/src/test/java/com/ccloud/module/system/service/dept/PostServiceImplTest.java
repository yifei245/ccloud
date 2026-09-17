package com.ccloud.module.system.service.dept;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.dal.dataobject.dept.PostDO;
import com.ccloud.module.system.dal.mysql.dept.PostMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.POST_NOT_ENABLE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.POST_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

@Import(PostServiceImpl.class)
class PostServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private PostServiceImpl postService;
    @Resource
    private PostMapper postMapper;

    @Test
    void validatePostList_三个分支() {
        PostDO ok = insert("ok", CommonStatusEnum.ENABLE);
        PostDO disabled = insert("off", CommonStatusEnum.DISABLE);

        postService.validatePostList(List.of(ok.getId()));
        postService.validatePostList(null);
        assertServiceException(() -> postService.validatePostList(List.of(-1L)), POST_NOT_FOUND);
        assertServiceException(() -> postService.validatePostList(List.of(disabled.getId())), POST_NOT_ENABLE);
    }

    @Test
    void getPostList() {
        PostDO a = insert("a", CommonStatusEnum.ENABLE);
        assertThat(postService.getPostList(List.of(a.getId()))).extracting(PostDO::getCode).containsExactly("a");
    }

    private PostDO insert(String code, CommonStatusEnum status) {
        PostDO post = new PostDO().setCode(code).setName(code).setSort(0).setStatus(status.getStatus());
        postMapper.insert(post);
        return post;
    }
}

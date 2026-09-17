package com.ccloud.module.system.service.dept;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.PostDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * 岗位 Service
 */
public interface PostService {

    Long createPost(@Valid PostSaveReqVO createReqVO);

    void updatePost(@Valid PostSaveReqVO updateReqVO);

    void deletePost(Long id);

    void deletePostList(List<Long> ids);

    PostDO getPost(Long id);

    List<PostDO> getPostList(Collection<Long> ids);

    /**
     * @param ids      为空时不按编号过滤
     * @param statuses 为空时不按状态过滤
     */
    List<PostDO> getPostList(Collection<Long> ids, Collection<Integer> statuses);

    PageResult<PostDO> getPostPage(PostPageReqVO reqVO);

    /**
     * 校验岗位均存在且开启；为空时不校验
     */
    void validatePostList(Collection<Long> ids);
}

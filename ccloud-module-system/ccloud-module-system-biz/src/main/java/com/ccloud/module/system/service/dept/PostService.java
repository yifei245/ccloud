package com.ccloud.module.system.service.dept;

import com.ccloud.module.system.dal.dataobject.dept.PostDO;

import java.util.Collection;
import java.util.List;

/**
 * 岗位 Service
 */
public interface PostService {

    List<PostDO> getPostList(Collection<Long> ids);

    /**
     * 校验岗位均存在且开启；为空时不校验
     */
    void validatePostList(Collection<Long> ids);
}

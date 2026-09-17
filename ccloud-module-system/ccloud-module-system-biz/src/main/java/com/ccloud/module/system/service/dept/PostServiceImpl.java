package com.ccloud.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.module.system.dal.dataobject.dept.PostDO;
import com.ccloud.module.system.dal.mysql.dept.PostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.POST_NOT_ENABLE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.POST_NOT_FOUND;

@Service
@Validated
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper postMapper;

    @Override
    public List<PostDO> getPostList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return postMapper.selectByIds(ids);
    }

    @Override
    public void validatePostList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Map<Long, PostDO> postMap = CollectionUtils.convertMap(postMapper.selectByIds(ids), PostDO::getId);
        for (Long id : ids) {
            PostDO post = postMap.get(id);
            if (post == null) {
                throw exception(POST_NOT_FOUND);
            }
            if (!CommonStatusEnum.isEnable(post.getStatus())) {
                throw exception(POST_NOT_ENABLE, post.getName());
            }
        }
    }
}

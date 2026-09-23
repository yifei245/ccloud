package com.ccloud.module.system.api.dept;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.dept.dto.PostRespDTO;
import com.ccloud.module.system.service.dept.PostService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class PostApiImpl implements PostApi {

    @Resource
    private PostService postService;

    @Override
    public CommonResult<List<PostRespDTO>> getPostList(Collection<Long> ids) {
        return success(BeanUtils.toBean(postService.getPostList(ids), PostRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> validPostList(Collection<Long> ids) {
        postService.validatePostList(ids);
        return success(true);
    }
}

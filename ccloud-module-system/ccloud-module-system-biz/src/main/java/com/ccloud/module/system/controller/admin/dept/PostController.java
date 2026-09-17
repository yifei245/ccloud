package com.ccloud.module.system.controller.admin.dept;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostRespVO;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.post.PostSimpleRespVO;
import com.ccloud.module.system.dal.dataobject.dept.PostDO;
import com.ccloud.module.system.service.dept.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 岗位")
@RestController
@RequestMapping("/admin-api/system/post")
@Validated
public class PostController {

    @Resource
    private PostService postService;

    @PostMapping("/create")
    @Operation(summary = "创建岗位")
    @PreAuthorize("@ss.hasPermission('system:post:create')")
    public CommonResult<Long> createPost(@Valid @RequestBody PostSaveReqVO createReqVO) {
        return success(postService.createPost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改岗位")
    @PreAuthorize("@ss.hasPermission('system:post:update')")
    public CommonResult<Boolean> updatePost(@Valid @RequestBody PostSaveReqVO updateReqVO) {
        postService.updatePost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除岗位")
    @Parameter(name = "id", description = "岗位编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:post:delete')")
    public CommonResult<Boolean> deletePost(@RequestParam("id") Long id) {
        postService.deletePost(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除岗位")
    @Parameter(name = "ids", description = "岗位编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:post:delete')")
    public CommonResult<Boolean> deletePostList(@RequestParam("ids") List<Long> ids) {
        postService.deletePostList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得岗位信息")
    @Parameter(name = "id", description = "岗位编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:post:query')")
    public CommonResult<PostRespVO> getPost(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(postService.getPost(id), PostRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取岗位精简信息列表", description = "只包含开启的岗位，主要用于前端的下拉选项")
    public CommonResult<List<PostSimpleRespVO>> getSimplePostList() {
        List<PostDO> list = postService.getPostList(null, Set.of(CommonStatusEnum.ENABLE.getStatus()));
        return success(BeanUtils.toBean(list, PostSimpleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得岗位分页列表")
    @PreAuthorize("@ss.hasPermission('system:post:query')")
    public CommonResult<PageResult<PostRespVO>> getPostPage(@Valid PostPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(postService.getPostPage(pageReqVO), PostRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "岗位管理导出")
    @PreAuthorize("@ss.hasPermission('system:post:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(HttpServletResponse response, @Valid PostPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PostDO> list = postService.getPostPage(reqVO).getList();
        ExcelUtils.write(response, "岗位数据.xls", "岗位列表", PostRespVO.class, BeanUtils.toBean(list, PostRespVO.class));
    }
}

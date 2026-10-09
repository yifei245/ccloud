package com.ccloud.module.member.controller.admin.tag;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagPageReqVO;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagRespVO;
import com.ccloud.module.member.controller.admin.tag.vo.MemberTagSaveReqVO;
import com.ccloud.module.member.service.tag.MemberTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 会员标签")
@RestController
@RequestMapping("/admin-api/member/tag")
@Validated
public class MemberTagController {

    @Resource
    private MemberTagService tagService;

    @PostMapping("/create")
    @Operation(summary = "创建会员标签")
    @PreAuthorize("@ss.hasPermission('member:tag:create')")
    public CommonResult<Long> createTag(@Valid @RequestBody MemberTagSaveReqVO createReqVO) {
        return success(tagService.createTag(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改会员标签")
    @PreAuthorize("@ss.hasPermission('member:tag:update')")
    public CommonResult<Boolean> updateTag(@Valid @RequestBody MemberTagSaveReqVO updateReqVO) {
        tagService.updateTag(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员标签", description = "仍有会员使用该标签时不能删除")
    @Parameter(name = "id", description = "标签编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:tag:delete')")
    public CommonResult<Boolean> deleteTag(@RequestParam("id") Long id) {
        tagService.deleteTag(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员标签")
    @Parameter(name = "id", description = "标签编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:tag:query')")
    public CommonResult<MemberTagRespVO> getTag(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(tagService.getTag(id), MemberTagRespVO.class));
    }

    @GetMapping("/list-all-simple")
    @Operation(summary = "获得全部会员标签", description = "主要用于前端的下拉选项")
    public CommonResult<List<MemberTagRespVO>> getSimpleTagList() {
        return success(BeanUtils.toBean(tagService.getTagList(), MemberTagRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员标签分页")
    @PreAuthorize("@ss.hasPermission('member:tag:query')")
    public CommonResult<PageResult<MemberTagRespVO>> getTagPage(@Valid MemberTagPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(tagService.getTagPage(pageReqVO), MemberTagRespVO.class));
    }
}

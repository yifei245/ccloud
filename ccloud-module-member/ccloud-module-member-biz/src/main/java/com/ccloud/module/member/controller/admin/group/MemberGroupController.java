package com.ccloud.module.member.controller.admin.group;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupPageReqVO;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupRespVO;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupSaveReqVO;
import com.ccloud.module.member.controller.admin.group.vo.MemberGroupSimpleRespVO;
import com.ccloud.module.member.service.group.MemberGroupService;
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

@Tag(name = "管理后台 - 会员分组")
@RestController
@RequestMapping("/admin-api/member/group")
@Validated
public class MemberGroupController {

    @Resource
    private MemberGroupService groupService;

    @PostMapping("/create")
    @Operation(summary = "创建会员分组")
    @PreAuthorize("@ss.hasPermission('member:group:create')")
    public CommonResult<Long> createGroup(@Valid @RequestBody MemberGroupSaveReqVO createReqVO) {
        return success(groupService.createGroup(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改会员分组")
    @PreAuthorize("@ss.hasPermission('member:group:update')")
    public CommonResult<Boolean> updateGroup(@Valid @RequestBody MemberGroupSaveReqVO updateReqVO) {
        groupService.updateGroup(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员分组", description = "仍有会员属于该分组时不能删除")
    @Parameter(name = "id", description = "分组编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:group:delete')")
    public CommonResult<Boolean> deleteGroup(@RequestParam("id") Long id) {
        groupService.deleteGroup(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员分组")
    @Parameter(name = "id", description = "分组编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:group:query')")
    public CommonResult<MemberGroupRespVO> getGroup(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(groupService.getGroup(id), MemberGroupRespVO.class));
    }

    @GetMapping("/list-all-simple")
    @Operation(summary = "获得启用的会员分组精简列表", description = "主要用于前端的下拉选项")
    public CommonResult<List<MemberGroupSimpleRespVO>> getSimpleGroupList() {
        return success(BeanUtils.toBean(groupService.getEnableGroupList(), MemberGroupSimpleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员分组分页")
    @PreAuthorize("@ss.hasPermission('member:group:query')")
    public CommonResult<PageResult<MemberGroupRespVO>> getGroupPage(@Valid MemberGroupPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(groupService.getGroupPage(pageReqVO), MemberGroupRespVO.class));
    }
}

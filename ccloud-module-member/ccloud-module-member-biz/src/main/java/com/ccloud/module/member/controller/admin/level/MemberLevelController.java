package com.ccloud.module.member.controller.admin.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelPageReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelRespVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSaveReqVO;
import com.ccloud.module.member.controller.admin.level.vo.MemberLevelSimpleRespVO;
import com.ccloud.module.member.service.level.MemberLevelService;
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

@Tag(name = "管理后台 - 会员等级")
@RestController
@RequestMapping("/admin-api/member/level")
@Validated
public class MemberLevelController {

    @Resource
    private MemberLevelService levelService;

    @PostMapping("/create")
    @Operation(summary = "创建会员等级")
    @PreAuthorize("@ss.hasPermission('member:level:create')")
    public CommonResult<Long> createLevel(@Valid @RequestBody MemberLevelSaveReqVO createReqVO) {
        return success(levelService.createLevel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改会员等级")
    @PreAuthorize("@ss.hasPermission('member:level:update')")
    public CommonResult<Boolean> updateLevel(@Valid @RequestBody MemberLevelSaveReqVO updateReqVO) {
        levelService.updateLevel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员等级", description = "仍有会员处于该等级时不能删除")
    @Parameter(name = "id", description = "等级编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:level:delete')")
    public CommonResult<Boolean> deleteLevel(@RequestParam("id") Long id) {
        levelService.deleteLevel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员等级")
    @Parameter(name = "id", description = "等级编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:level:query')")
    public CommonResult<MemberLevelRespVO> getLevel(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(levelService.getLevel(id), MemberLevelRespVO.class));
    }

    @GetMapping("/list-all-simple")
    @Operation(summary = "获得启用的会员等级精简列表", description = "按等级序号升序，主要用于前端的下拉选项")
    public CommonResult<List<MemberLevelSimpleRespVO>> getSimpleLevelList() {
        return success(BeanUtils.toBean(levelService.getEnableLevelList(), MemberLevelSimpleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员等级分页")
    @PreAuthorize("@ss.hasPermission('member:level:query')")
    public CommonResult<PageResult<MemberLevelRespVO>> getLevelPage(@Valid MemberLevelPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(levelService.getLevelPage(pageReqVO), MemberLevelRespVO.class));
    }
}

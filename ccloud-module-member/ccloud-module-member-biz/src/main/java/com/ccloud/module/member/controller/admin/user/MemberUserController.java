package com.ccloud.module.member.controller.admin.user;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.common.util.string.StrUtils;
import com.ccloud.module.member.controller.admin.user.vo.*;
import com.ccloud.module.member.dal.dataobject.group.MemberGroupDO;
import com.ccloud.module.member.dal.dataobject.level.MemberLevelDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.service.group.MemberGroupService;
import com.ccloud.module.member.service.level.MemberLevelService;
import com.ccloud.module.member.service.point.MemberPointService;
import com.ccloud.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 会员用户")
@RestController
@RequestMapping("/admin-api/member/user")
@Validated
public class MemberUserController {

    @Resource
    private MemberUserService userService;
    @Resource
    private MemberLevelService levelService;
    @Resource
    private MemberGroupService groupService;
    @Resource
    private MemberPointService pointService;

    @GetMapping("/page")
    @Operation(summary = "获得会员用户分页")
    @PreAuthorize("@ss.hasPermission('member:user:query')")
    public CommonResult<PageResult<MemberUserRespVO>> getUserPage(@Valid MemberUserPageReqVO pageReqVO) {
        PageResult<MemberUserDO> page = userService.getUserPage(pageReqVO);
        return success(new PageResult<>(buildRespList(page.getList()), page.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员用户")
    @Parameter(name = "id", description = "用户编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:user:query')")
    public CommonResult<MemberUserRespVO> getUser(@RequestParam("id") Long id) {
        MemberUserDO user = userService.getUser(id);
        return success(user == null ? null : buildRespList(List.of(user)).getFirst());
    }

    @PutMapping("/update")
    @Operation(summary = "修改会员用户", description = "改为关闭状态时作废该会员的全部令牌")
    @PreAuthorize("@ss.hasPermission('member:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody MemberUserUpdateReqVO updateReqVO) {
        userService.updateUserByAdmin(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-point")
    @Operation(summary = "修改会员积分", description = "正数增加、负数扣减，调整后不能小于 0")
    @PreAuthorize("@ss.hasPermission('member:user:update-point')")
    public CommonResult<Boolean> updateUserPoint(@Valid @RequestBody MemberUserUpdatePointReqVO reqVO) {
        pointService.updateUserPointByAdmin(reqVO.getId(), reqVO.getPoint());
        return success(true);
    }

    @PutMapping("/update-level")
    @Operation(summary = "修改会员等级", description = "经验同步调到该等级的门槛")
    @PreAuthorize("@ss.hasPermission('member:user:update-level')")
    public CommonResult<Boolean> updateUserLevel(@Valid @RequestBody MemberUserUpdateLevelReqVO reqVO) {
        levelService.updateUserLevel(reqVO.getId(), reqVO.getLevelId(), reqVO.getReason());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员用户", description = "同时作废该会员的全部令牌")
    @Parameter(name = "id", description = "用户编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('member:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    /**
     * 批量补上等级、分组名称；tag_ids 为逗号分隔的字符串，转成编号列表
     */
    private List<MemberUserRespVO> buildRespList(List<MemberUserDO> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MemberLevelDO> levels = CollectionUtils.convertMap(levelService.getLevelList(CollectionUtils.convertSet(
                CollectionUtils.filterList(users, u -> u.getLevelId() != null), MemberUserDO::getLevelId)), MemberLevelDO::getId);
        Map<Long, MemberGroupDO> groups = CollectionUtils.convertMap(groupService.getGroupList(CollectionUtils.convertSet(
                CollectionUtils.filterList(users, u -> u.getGroupId() != null), MemberUserDO::getGroupId)), MemberGroupDO::getId);
        return CollectionUtils.convertList(users, user -> {
            MemberUserRespVO respVO = BeanUtils.toBean(user, MemberUserRespVO.class);
            respVO.setTagIds(StrUtil.isEmpty(user.getTagIds()) ? Collections.emptyList()
                    : StrUtils.splitToLong(user.getTagIds(), ","));
            MemberLevelDO level = levels.get(user.getLevelId());
            respVO.setLevelName(level == null ? null : level.getName());
            MemberGroupDO group = groups.get(user.getGroupId());
            respVO.setGroupName(group == null ? null : group.getName());
            return respVO;
        });
    }
}

package com.ccloud.module.member.controller.app.level;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.security.core.util.SecurityFrameworkUtils;
import com.ccloud.module.member.controller.app.level.vo.AppMemberLevelRespVO;
import com.ccloud.module.member.controller.app.level.vo.AppMemberMyLevelRespVO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.service.level.MemberLevelService;
import com.ccloud.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 会员等级")
@RestController
@RequestMapping("/app-api/member/level")
@Validated
public class AppMemberLevelController {

    @Resource
    private MemberLevelService levelService;
    @Resource
    private MemberUserService userService;

    @GetMapping("/list")
    @Operation(summary = "获得启用的会员等级列表", description = "按等级序号升序")
    public CommonResult<List<AppMemberLevelRespVO>> getLevelList() {
        return success(BeanUtils.toBean(levelService.getEnableLevelList(), AppMemberLevelRespVO.class));
    }

    @GetMapping("/get-my")
    @Operation(summary = "获得我的等级与经验")
    public CommonResult<AppMemberMyLevelRespVO> getMyLevel() {
        MemberUserDO user = userService.validateUserExists(SecurityFrameworkUtils.getLoginUserId());
        AppMemberMyLevelRespVO respVO = new AppMemberMyLevelRespVO();
        respVO.setExperience(user.getExperience());
        if (user.getLevelId() != null) {
            respVO.setLevel(BeanUtils.toBean(levelService.getLevel(user.getLevelId()), AppMemberLevelRespVO.class));
        }
        return success(respVO);
    }
}

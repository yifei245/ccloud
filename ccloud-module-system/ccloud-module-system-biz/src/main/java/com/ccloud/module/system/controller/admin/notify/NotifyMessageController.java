package com.ccloud.module.system.controller.admin.notify;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.message.NotifyMessageRespVO;
import com.ccloud.module.system.service.notify.NotifyMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 站内信。"我的站内信"接口只需登录，按登录用户限定范围，无需额外权限
 */
@Tag(name = "管理后台 - 站内信")
@RestController
@RequestMapping("/admin-api/system/notify-message")
@Validated
public class NotifyMessageController {

    private static final Integer ADMIN = UserTypeEnum.ADMIN.getValue();

    @Resource
    private NotifyMessageService notifyMessageService;

    // ========== 管理：全部站内信 ==========

    @GetMapping("/get")
    @Operation(summary = "获得站内信")
    @Parameter(name = "id", description = "站内信编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notify-message:query')")
    public CommonResult<NotifyMessageRespVO> getNotifyMessage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(notifyMessageService.getNotifyMessage(id), NotifyMessageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得站内信分页")
    @PreAuthorize("@ss.hasPermission('system:notify-message:query')")
    public CommonResult<PageResult<NotifyMessageRespVO>> getNotifyMessagePage(@Valid NotifyMessagePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(notifyMessageService.getNotifyMessagePage(pageReqVO), NotifyMessageRespVO.class));
    }

    // ========== 我的站内信 ==========

    @GetMapping("/my-page")
    @Operation(summary = "获得我的站内信分页")
    public CommonResult<PageResult<NotifyMessageRespVO>> getMyNotifyMessagePage(@Valid NotifyMessageMyPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(notifyMessageService.getMyNotifyMessagePage(pageReqVO, getLoginUserId(), ADMIN),
                NotifyMessageRespVO.class));
    }

    @PutMapping("/update-read")
    @Operation(summary = "标记站内信为已读", description = "只会更新属于自己的消息")
    @Parameter(name = "ids", description = "站内信编号数组", required = true, example = "1024,2048")
    public CommonResult<Boolean> updateNotifyMessageRead(@RequestParam("ids") List<Long> ids) {
        notifyMessageService.updateNotifyMessageRead(ids, getLoginUserId(), ADMIN);
        return success(true);
    }

    @PutMapping("/update-all-read")
    @Operation(summary = "标记所有站内信为已读")
    public CommonResult<Boolean> updateAllNotifyMessageRead() {
        notifyMessageService.updateAllNotifyMessageRead(getLoginUserId(), ADMIN);
        return success(true);
    }

    @GetMapping("/get-unread-list")
    @Operation(summary = "获取当前用户的最新未读站内信列表")
    @Parameter(name = "size", description = "获取条数", example = "10")
    public CommonResult<List<NotifyMessageRespVO>> getUnreadNotifyMessageList(
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return success(BeanUtils.toBean(notifyMessageService.getUnreadNotifyMessageList(getLoginUserId(), ADMIN, size),
                NotifyMessageRespVO.class));
    }

    @GetMapping("/get-unread-count")
    @Operation(summary = "获得当前用户的未读站内信数量")
    public CommonResult<Long> getUnreadNotifyMessageCount() {
        return success(notifyMessageService.getUnreadNotifyMessageCount(getLoginUserId(), ADMIN));
    }
}

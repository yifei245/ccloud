package com.ccloud.module.open.controller.admin.notify;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.open.controller.admin.notify.vo.OpenNotifyLogPageReqVO;
import com.ccloud.module.open.controller.admin.notify.vo.OpenNotifyLogRespVO;
import com.ccloud.module.open.dal.dataobject.notify.OpenNotifyLogDO;
import com.ccloud.module.open.service.notify.OpenNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 开放应用回调日志")
@RestController
@RequestMapping("/admin-api/open/notify-log")
@Validated
public class OpenNotifyLogController {

    @Resource
    private OpenNotifyService openNotifyService;

    @GetMapping("/page")
    @Operation(summary = "获得回调日志分页", description = "按租户隔离，仅看到本租户应用的回调")
    @PreAuthorize("@ss.hasPermission('open:notify-log:query')")
    public CommonResult<PageResult<OpenNotifyLogRespVO>> getNotifyLogPage(@Valid OpenNotifyLogPageReqVO pageReqVO) {
        PageResult<OpenNotifyLogDO> page = openNotifyService.getNotifyLogPage(pageReqVO);
        return success(BeanUtils.toBean(page, OpenNotifyLogRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得回调日志", description = "含回调请求体与对方最近一次响应")
    @Parameter(name = "id", description = "日志编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:notify-log:query')")
    public CommonResult<OpenNotifyLogRespVO> getNotifyLog(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(openNotifyService.getNotifyLog(id), OpenNotifyLogRespVO.class));
    }

    @PutMapping("/retry")
    @Operation(summary = "重新通知", description = "只限失败待重试与放弃的回调；重试次数清零后立即发送")
    @Parameter(name = "id", description = "日志编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('open:notify-log:retry')")
    public CommonResult<Boolean> retryNotify(@RequestParam("id") Long id) {
        openNotifyService.retryNotify(id);
        return success(true);
    }
}

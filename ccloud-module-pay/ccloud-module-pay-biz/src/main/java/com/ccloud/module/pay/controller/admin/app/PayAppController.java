package com.ccloud.module.pay.controller.admin.app;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppPageReqVO;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppRespVO;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.service.app.PayAppService;
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

@Tag(name = "管理后台 - 支付应用")
@RestController
@RequestMapping("/admin-api/pay/app")
@Validated
public class PayAppController {

    @Resource
    private PayAppService payAppService;

    @PostMapping("/create")
    @Operation(summary = "创建支付应用")
    @PreAuthorize("@ss.hasPermission('pay:app:create')")
    public CommonResult<Long> createApp(@Valid @RequestBody PayAppSaveReqVO createReqVO) {
        return success(payAppService.createApp(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改支付应用", description = "不能修改应用标识")
    @PreAuthorize("@ss.hasPermission('pay:app:update')")
    public CommonResult<Boolean> updateApp(@Valid @RequestBody PayAppSaveReqVO updateReqVO) {
        payAppService.updateApp(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除支付应用", description = "同时删除它的渠道")
    @Parameter(name = "id", description = "应用编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:app:delete')")
    public CommonResult<Boolean> deleteApp(@RequestParam("id") Long id) {
        payAppService.deleteApp(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得支付应用")
    @Parameter(name = "id", description = "应用编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pay:app:query')")
    public CommonResult<PayAppRespVO> getApp(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(payAppService.getApp(id), PayAppRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得支付应用分页")
    @PreAuthorize("@ss.hasPermission('pay:app:query')")
    public CommonResult<PageResult<PayAppRespVO>> getAppPage(@Valid PayAppPageReqVO pageReqVO) {
        PageResult<PayAppDO> page = payAppService.getAppPage(pageReqVO);
        return success(BeanUtils.toBean(page, PayAppRespVO.class));
    }

    @GetMapping("/list-all-simple")
    @Operation(summary = "获得支付应用列表", description = "不分页，供下拉选择")
    @PreAuthorize("@ss.hasPermission('pay:app:query')")
    public CommonResult<List<PayAppRespVO>> getAppList() {
        return success(BeanUtils.toBean(payAppService.getAppList(), PayAppRespVO.class));
    }
}

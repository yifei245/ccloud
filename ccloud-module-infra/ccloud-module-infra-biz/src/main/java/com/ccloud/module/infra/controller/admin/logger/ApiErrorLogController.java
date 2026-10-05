package com.ccloud.module.infra.controller.admin.logger;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiErrorLogPageReqVO;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiErrorLogRespVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiErrorLogDO;
import com.ccloud.module.infra.service.logger.ApiErrorLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - API 异常日志")
@RestController
@RequestMapping("/admin-api/infra/api-error-log")
@Validated
public class ApiErrorLogController {

    @Resource
    private ApiErrorLogService apiErrorLogService;

    @PutMapping("/update-status")
    @Operation(summary = "处理异常日志", description = "只能把未处理的日志标记为已处理或已忽略")
    @Parameter(name = "id", description = "日志编号", required = true, example = "1024")
    @Parameter(name = "processStatus", description = "处理状态：1 已处理 2 已忽略", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('infra:api-error-log:update-status')")
    public CommonResult<Boolean> updateApiErrorLogProcess(@RequestParam("id") Long id,
                                                          @RequestParam("processStatus") Integer processStatus) {
        apiErrorLogService.updateApiErrorLogProcess(id, processStatus, getLoginUserId());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 API 异常日志分页")
    @PreAuthorize("@ss.hasPermission('infra:api-error-log:query')")
    public CommonResult<PageResult<ApiErrorLogRespVO>> getApiErrorLogPage(@Valid ApiErrorLogPageReqVO pageReqVO) {
        PageResult<ApiErrorLogDO> page = apiErrorLogService.getApiErrorLogPage(pageReqVO);
        return success(BeanUtils.toBean(page, ApiErrorLogRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出 API 异常日志 Excel", description = "不含请求参数与异常堆栈")
    @PreAuthorize("@ss.hasPermission('infra:api-error-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportApiErrorLog(HttpServletResponse response, @Valid ApiErrorLogPageReqVO pageReqVO) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ApiErrorLogDO> list = apiErrorLogService.getApiErrorLogPage(pageReqVO).getList();
        ExcelUtils.write(response, "API 异常日志.xls", "数据", ApiErrorLogRespVO.class,
                BeanUtils.toBean(list, ApiErrorLogRespVO.class));
    }
}

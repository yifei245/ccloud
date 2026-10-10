package com.ccloud.module.open.controller.admin.logger;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogPageReqVO;
import com.ccloud.module.open.controller.admin.logger.vo.OpenApiLogRespVO;
import com.ccloud.module.open.dal.dataobject.logger.OpenApiLogDO;
import com.ccloud.module.open.service.logger.OpenApiLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 开放接口调用日志")
@RestController
@RequestMapping("/admin-api/open/api-log")
@Validated
public class OpenApiLogController {

    @Resource
    private OpenApiLogService openApiLogService;

    @GetMapping("/page")
    @Operation(summary = "获得开放接口调用日志分页", description = "按租户隔离，仅看到本租户应用的调用日志")
    @PreAuthorize("@ss.hasPermission('open:api-log:query')")
    public CommonResult<PageResult<OpenApiLogRespVO>> getApiLogPage(@Valid OpenApiLogPageReqVO pageReqVO) {
        PageResult<OpenApiLogDO> page = openApiLogService.getApiLogPage(pageReqVO);
        return success(BeanUtils.toBean(page, OpenApiLogRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出开放接口调用日志 Excel", description = "不含请求参数")
    @PreAuthorize("@ss.hasPermission('open:api-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportApiLog(HttpServletResponse response, @Valid OpenApiLogPageReqVO pageReqVO) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OpenApiLogDO> list = openApiLogService.getApiLogPage(pageReqVO).getList();
        ExcelUtils.write(response, "开放接口调用日志.xls", "数据", OpenApiLogRespVO.class,
                BeanUtils.toBean(list, OpenApiLogRespVO.class));
    }
}

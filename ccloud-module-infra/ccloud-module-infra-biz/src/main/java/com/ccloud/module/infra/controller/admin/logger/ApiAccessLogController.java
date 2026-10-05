package com.ccloud.module.infra.controller.admin.logger;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiAccessLogPageReqVO;
import com.ccloud.module.infra.controller.admin.logger.vo.ApiAccessLogRespVO;
import com.ccloud.module.infra.dal.dataobject.logger.ApiAccessLogDO;
import com.ccloud.module.infra.service.logger.ApiAccessLogService;
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

@Tag(name = "管理后台 - API 访问日志")
@RestController
@RequestMapping("/admin-api/infra/api-access-log")
@Validated
public class ApiAccessLogController {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @GetMapping("/page")
    @Operation(summary = "获得 API 访问日志分页", description = "跨租户、跨服务的访问日志，按租户隔离后仅看到本租户的")
    @PreAuthorize("@ss.hasPermission('infra:api-access-log:query')")
    public CommonResult<PageResult<ApiAccessLogRespVO>> getApiAccessLogPage(@Valid ApiAccessLogPageReqVO pageReqVO) {
        PageResult<ApiAccessLogDO> page = apiAccessLogService.getApiAccessLogPage(pageReqVO);
        return success(BeanUtils.toBean(page, ApiAccessLogRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出 API 访问日志 Excel", description = "不含请求参数与响应结果")
    @PreAuthorize("@ss.hasPermission('infra:api-access-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportApiAccessLog(HttpServletResponse response, @Valid ApiAccessLogPageReqVO pageReqVO) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ApiAccessLogDO> list = apiAccessLogService.getApiAccessLogPage(pageReqVO).getList();
        ExcelUtils.write(response, "API 访问日志.xls", "数据", ApiAccessLogRespVO.class,
                BeanUtils.toBean(list, ApiAccessLogRespVO.class));
    }
}

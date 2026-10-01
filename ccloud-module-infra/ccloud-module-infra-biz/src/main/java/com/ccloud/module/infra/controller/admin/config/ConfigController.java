package com.ccloud.module.infra.controller.admin.config;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigRespVO;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.config.ConfigDO;
import com.ccloud.module.infra.service.config.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.CONFIG_GET_VALUE_ERROR_IF_VISIBLE;

@Tag(name = "管理后台 - 参数配置")
@RestController
@RequestMapping("/admin-api/infra/config")
@Validated
public class ConfigController {

    @Resource
    private ConfigService configService;

    @PostMapping("/create")
    @Operation(summary = "创建参数配置", description = "类型固定为自定义")
    @PreAuthorize("@ss.hasPermission('infra:config:create')")
    public CommonResult<Long> createConfig(@Valid @RequestBody ConfigSaveReqVO createReqVO) {
        return success(configService.createConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改参数配置", description = "系统内置参数不能改键名")
    @PreAuthorize("@ss.hasPermission('infra:config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody ConfigSaveReqVO updateReqVO) {
        configService.updateConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除参数配置", description = "系统内置参数不能删除")
    @Parameter(name = "id", description = "参数编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        configService.deleteConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得参数配置")
    @Parameter(name = "id", description = "参数编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:config:query')")
    public CommonResult<ConfigRespVO> getConfig(@RequestParam("id") Long id) {
        return success(toRespVO(configService.getConfig(id)));
    }

    @GetMapping("/get-value-by-key")
    @Operation(summary = "根据参数键名查询参数值", description = "只能读取可见参数；参数不存在时返回空")
    @Parameter(name = "key", description = "参数键", required = true, example = "system.auth.register-enabled")
    public CommonResult<String> getConfigKey(@RequestParam("key") String key) {
        ConfigDO config = configService.getConfigByKey(key);
        if (config == null) {
            return success(null);
        }
        if (!Boolean.TRUE.equals(config.getVisible())) {
            throw exception(CONFIG_GET_VALUE_ERROR_IF_VISIBLE);
        }
        return success(config.getValue());
    }

    @GetMapping("/page")
    @Operation(summary = "获得参数配置分页")
    @PreAuthorize("@ss.hasPermission('infra:config:query')")
    public CommonResult<PageResult<ConfigRespVO>> getConfigPage(@Valid ConfigPageReqVO pageReqVO) {
        PageResult<ConfigDO> page = configService.getConfigPage(pageReqVO);
        return success(new PageResult<>(toRespVOList(page.getList()), page.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出参数配置 Excel")
    @PreAuthorize("@ss.hasPermission('infra:config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfig(HttpServletResponse response, @Valid ConfigPageReqVO pageReqVO) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ConfigRespVO> list = toRespVOList(configService.getConfigPage(pageReqVO).getList());
        ExcelUtils.write(response, "参数配置.xls", "参数列表", ConfigRespVO.class, list);
    }

    private static List<ConfigRespVO> toRespVOList(List<ConfigDO> list) {
        return list.stream().map(ConfigController::toRespVO).toList();
    }

    /**
     * DO 的字段叫 configKey，VO 对外叫 key，同名拷贝带不上，需手工赋值
     */
    private static ConfigRespVO toRespVO(ConfigDO config) {
        if (config == null) {
            return null;
        }
        ConfigRespVO respVO = BeanUtils.toBean(config, ConfigRespVO.class);
        respVO.setKey(config.getConfigKey());
        return respVO;
    }
}

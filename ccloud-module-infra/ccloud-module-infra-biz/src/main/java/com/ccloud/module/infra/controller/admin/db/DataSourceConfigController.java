package com.ccloud.module.infra.controller.admin.db;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.db.vo.DataSourceConfigRespVO;
import com.ccloud.module.infra.controller.admin.db.vo.DataSourceConfigSaveReqVO;
import com.ccloud.module.infra.service.db.DataSourceConfigService;
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

@Tag(name = "管理后台 - 数据源配置")
@RestController
@RequestMapping("/admin-api/infra/data-source-config")
@Validated
public class DataSourceConfigController {

    @Resource
    private DataSourceConfigService dataSourceConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建数据源配置", description = "连接测试通过才保存，密码加密存储")
    @PreAuthorize("@ss.hasPermission('infra:data-source-config:create')")
    public CommonResult<Long> createDataSourceConfig(@Valid @RequestBody DataSourceConfigSaveReqVO createReqVO) {
        return success(dataSourceConfigService.createDataSourceConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改数据源配置", description = "主数据源不能修改；密码提交掩码或留空表示沿用原密码")
    @PreAuthorize("@ss.hasPermission('infra:data-source-config:update')")
    public CommonResult<Boolean> updateDataSourceConfig(@Valid @RequestBody DataSourceConfigSaveReqVO updateReqVO) {
        dataSourceConfigService.updateDataSourceConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除数据源配置", description = "主数据源不能删除；已有代码生成表引用时不能删除")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:data-source-config:delete')")
    public CommonResult<Boolean> deleteDataSourceConfig(@RequestParam("id") Long id) {
        dataSourceConfigService.deleteDataSourceConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得数据源配置", description = "不返回密码")
    @Parameter(name = "id", description = "编号，0 为主数据源", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:data-source-config:query')")
    public CommonResult<DataSourceConfigRespVO> getDataSourceConfig(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(dataSourceConfigService.getDataSourceConfig(id), DataSourceConfigRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得数据源配置列表", description = "主数据源在首位；不返回密码")
    @PreAuthorize("@ss.hasPermission('infra:data-source-config:query')")
    public CommonResult<List<DataSourceConfigRespVO>> getDataSourceConfigList() {
        return success(BeanUtils.toBean(dataSourceConfigService.getDataSourceConfigList(), DataSourceConfigRespVO.class));
    }
}

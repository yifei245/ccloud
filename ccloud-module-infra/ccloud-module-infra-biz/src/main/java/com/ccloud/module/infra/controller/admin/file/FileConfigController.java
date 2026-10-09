package com.ccloud.module.infra.controller.admin.file;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigRespVO;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;
import com.ccloud.module.infra.framework.file.core.enums.FileStorageEnum;
import com.ccloud.module.infra.service.file.FileConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 文件配置")
@RestController
@RequestMapping("/admin-api/infra/file-config")
@Validated
public class FileConfigController {

    @Resource
    private FileConfigService fileConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建文件配置", description = "没有主配置时自动成为主配置")
    @PreAuthorize("@ss.hasPermission('infra:file-config:create')")
    public CommonResult<Long> createFileConfig(@Valid @RequestBody FileConfigSaveReqVO createReqVO) {
        return success(fileConfigService.createFileConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改文件配置", description = "不改变是否为主配置；密钥项提交掩码表示沿用原值")
    @PreAuthorize("@ss.hasPermission('infra:file-config:update')")
    public CommonResult<Boolean> updateFileConfig(@Valid @RequestBody FileConfigSaveReqVO updateReqVO) {
        fileConfigService.updateFileConfig(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-master")
    @Operation(summary = "设为主配置", description = "上传默认使用主配置，全局只有一个")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('infra:file-config:update')")
    public CommonResult<Boolean> updateFileConfigMaster(@RequestParam("id") Long id) {
        fileConfigService.updateFileConfigMaster(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件配置", description = "主配置不能删除")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('infra:file-config:delete')")
    public CommonResult<Boolean> deleteFileConfig(@RequestParam("id") Long id) {
        fileConfigService.deleteFileConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得文件配置", description = "密钥项显示为掩码")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('infra:file-config:query')")
    public CommonResult<FileConfigRespVO> getFileConfig(@RequestParam("id") Long id) {
        return success(toRespVO(fileConfigService.getFileConfig(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得文件配置分页")
    @PreAuthorize("@ss.hasPermission('infra:file-config:query')")
    public CommonResult<PageResult<FileConfigRespVO>> getFileConfigPage(@Valid FileConfigPageReqVO pageReqVO) {
        PageResult<FileConfigDO> page = fileConfigService.getFileConfigPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream().map(FileConfigController::toRespVO).toList(), page.getTotal()));
    }

    @GetMapping("/test")
    @Operation(summary = "测试文件配置是否可用", description = "上传一个测试文件，返回其访问地址")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('infra:file-config:query')")
    public CommonResult<String> testFileConfig(@RequestParam("id") Long id) {
        return success(fileConfigService.testFileConfig(id));
    }

    /**
     * config 在库里是 JSON 字符串，对外解析为 Map，并对存储器声明的密钥项掩码
     */
    private static FileConfigRespVO toRespVO(FileConfigDO fileConfig) {
        if (fileConfig == null) {
            return null;
        }
        Map<String, Object> config = new HashMap<>(
                JsonUtils.parseObject(fileConfig.getConfig(), new TypeReference<Map<String, Object>>() {}));
        FileStorageEnum storage = FileStorageEnum.valueOf(fileConfig.getStorage());
        if (storage != null) {
            storage.getSecretKeys().forEach(key -> config.computeIfPresent(key, (k, v) -> DesensitizeUtils.MASK));
        }
        // 不能用 BeanUtils.toBean：DO 的 config 是 JSON 字符串，VO 的是 Map，同名拷贝会因类型不符失败
        FileConfigRespVO respVO = new FileConfigRespVO();
        respVO.setId(fileConfig.getId());
        respVO.setName(fileConfig.getName());
        respVO.setStorage(fileConfig.getStorage());
        respVO.setMaster(fileConfig.getMaster());
        respVO.setConfig(config);
        respVO.setRemark(fileConfig.getRemark());
        respVO.setCreateTime(fileConfig.getCreateTime());
        return respVO;
    }
}

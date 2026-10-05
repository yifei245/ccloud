package com.ccloud.module.infra.controller.admin.codegen;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.codegen.vo.*;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.service.codegen.CodegenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.pojo.CommonResult.success;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.CODEGEN_TABLE_NOT_EXISTS;

@Tag(name = "管理后台 - 代码生成")
@RestController
@RequestMapping("/admin-api/infra/codegen")
@Validated
public class CodegenController {

    @Resource
    private CodegenService codegenService;

    @GetMapping("/db/table/list")
    @Operation(summary = "获得数据库中尚未导入的表", description = "按表名、表注释模糊筛选，已导入的表不返回")
    @Parameter(name = "dataSourceConfigId", description = "数据源配置编号，0 为主数据源", required = true, example = "0")
    @Parameter(name = "name", description = "表名，模糊匹配", example = "infra_demo")
    @Parameter(name = "comment", description = "表注释，模糊匹配", example = "学生")
    @PreAuthorize("@ss.hasPermission('infra:codegen:create')")
    public CommonResult<List<DatabaseTableRespVO>> getDatabaseTableList(
            @RequestParam(value = "dataSourceConfigId", defaultValue = "0") Long dataSourceConfigId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "comment", required = false) String comment) {
        return success(BeanUtils.toBean(codegenService.getDatabaseTableList(dataSourceConfigId, name, comment),
                DatabaseTableRespVO.class));
    }

    @PostMapping("/create-list")
    @Operation(summary = "导入数据库中的表，按表结构推断默认的生成配置")
    @PreAuthorize("@ss.hasPermission('infra:codegen:create')")
    public CommonResult<List<Long>> createCodegenList(@Valid @RequestBody CodegenCreateListReqVO reqVO) {
        return success(codegenService.createCodegenList(reqVO.getDataSourceConfigId(), reqVO.getTableNames()));
    }

    @GetMapping("/table/page")
    @Operation(summary = "获得表定义分页")
    @PreAuthorize("@ss.hasPermission('infra:codegen:query')")
    public CommonResult<PageResult<CodegenTableRespVO>> getCodegenTablePage(@Valid CodegenTablePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(codegenService.getCodegenTablePage(pageReqVO), CodegenTableRespVO.class));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得表定义与字段定义")
    @Parameter(name = "tableId", description = "表编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:codegen:query')")
    public CommonResult<CodegenDetailRespVO> getCodegenDetail(@RequestParam("tableId") Long tableId) {
        CodegenTableDO table = codegenService.getCodegenTable(tableId);
        if (table == null) {
            throw exception(CODEGEN_TABLE_NOT_EXISTS);
        }
        CodegenDetailRespVO detail = new CodegenDetailRespVO();
        detail.setTable(BeanUtils.toBean(table, CodegenTableRespVO.class));
        detail.setColumns(BeanUtils.toBean(codegenService.getCodegenColumnListByTableId(tableId), CodegenColumnRespVO.class));
        return success(detail);
    }

    @PutMapping("/update")
    @Operation(summary = "修改表定义与字段定义")
    @PreAuthorize("@ss.hasPermission('infra:codegen:update')")
    public CommonResult<Boolean> updateCodegen(@Valid @RequestBody CodegenUpdateReqVO reqVO) {
        codegenService.updateCodegen(reqVO);
        return success(true);
    }

    @PutMapping("/sync-from-db")
    @Operation(summary = "按数据库当前表结构同步字段定义", description = "保留用户在生成配置里的编辑；表结构没有变化时报错")
    @Parameter(name = "tableId", description = "表编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:codegen:update')")
    public CommonResult<Boolean> syncCodegenFromDB(@RequestParam("tableId") Long tableId) {
        codegenService.syncCodegenFromDB(tableId);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除表定义与字段定义")
    @Parameter(name = "tableId", description = "表编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:codegen:delete')")
    public CommonResult<Boolean> deleteCodegen(@RequestParam("tableId") Long tableId) {
        codegenService.deleteCodegen(tableId);
        return success(true);
    }

    @GetMapping("/preview")
    @Operation(summary = "预览生成的代码")
    @Parameter(name = "tableId", description = "表编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:codegen:preview')")
    public CommonResult<List<CodegenPreviewRespVO>> previewCodegen(@RequestParam("tableId") Long tableId) {
        return success(codegenService.generationCodes(tableId).entrySet().stream().map(entry -> {
            CodegenPreviewRespVO preview = new CodegenPreviewRespVO();
            preview.setFilePath(entry.getKey());
            preview.setCode(entry.getValue());
            return preview;
        }).toList());
    }

    @GetMapping("/download")
    @Operation(summary = "下载生成的代码", description = "zip 压缩包，条目名即文件路径")
    @Parameter(name = "tableId", description = "表编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('infra:codegen:download')")
    @ApiAccessLog(operateType = EXPORT)
    public void downloadCodegen(@RequestParam("tableId") Long tableId, HttpServletResponse response) throws IOException {
        CodegenTableDO table = codegenService.getCodegenTable(tableId);
        if (table == null) {
            throw exception(CODEGEN_TABLE_NOT_EXISTS);
        }
        Map<String, String> codes = codegenService.generationCodes(tableId);
        response.setContentType("application/zip");
        // 表名为 ASCII 时输出普通的 filename 参数，其余情况按 RFC 5987 编码
        String filename = "codegen-" + table.getTableName() + ".zip";
        ContentDisposition.Builder disposition = ContentDisposition.attachment();
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, (StandardCharsets.US_ASCII.newEncoder().canEncode(filename)
                ? disposition.filename(filename) : disposition.filename(filename, StandardCharsets.UTF_8)).build().toString());
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> code : codes.entrySet()) {
                zip.putNextEntry(new ZipEntry(code.getKey()));
                zip.write(code.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}

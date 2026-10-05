package com.ccloud.module.infra.controller.admin.db;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.module.infra.service.db.DbDocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Tag(name = "管理后台 - 数据库文档")
@RestController
@RequestMapping("/admin-api/infra/db-doc")
@Validated
public class DbDocController {

    @Resource
    private DbDocService dbDocService;

    @GetMapping("/export-html")
    @Operation(summary = "导出 HTML 格式的数据库文档")
    @PreAuthorize("@ss.hasPermission('infra:db-doc:query')")
    @ApiAccessLog(responseEnable = false)
    public void exportHtml(@RequestParam(value = "dataSourceConfigId", defaultValue = "0") Long dataSourceConfigId,
                           @RequestParam(value = "download", defaultValue = "false") boolean download,
                           HttpServletResponse response) throws IOException {
        write(response, dbDocService.exportHtml(dataSourceConfigId), "text/html;charset=UTF-8", "数据库文档.html", download);
    }

    @GetMapping("/export-markdown")
    @Operation(summary = "导出 Markdown 格式的数据库文档")
    @PreAuthorize("@ss.hasPermission('infra:db-doc:query')")
    @ApiAccessLog(responseEnable = false)
    public void exportMarkdown(@RequestParam(value = "dataSourceConfigId", defaultValue = "0") Long dataSourceConfigId,
                               @RequestParam(value = "download", defaultValue = "false") boolean download,
                               HttpServletResponse response) throws IOException {
        write(response, dbDocService.exportMarkdown(dataSourceConfigId), "text/markdown;charset=UTF-8", "数据库文档.md",
                download);
    }

    private static void write(HttpServletResponse response, String content, String contentType, String filename,
                              boolean download) throws IOException {
        response.setContentType(contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 文档内容来自数据库注释，即使转义有遗漏，也禁止执行任何脚本
        response.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'");
        if (download) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8).build().toString());
        }
        response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
    }
}

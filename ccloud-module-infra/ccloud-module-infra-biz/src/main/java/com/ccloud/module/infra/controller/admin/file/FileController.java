package com.ccloud.module.infra.controller.admin.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.file.vo.FilePageReqVO;
import com.ccloud.module.infra.controller.admin.file.vo.FileRespVO;
import com.ccloud.module.infra.framework.file.core.utils.FileTypeUtils;
import com.ccloud.module.infra.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 文件")
@RestController
@RequestMapping("/admin-api/infra/file")
@Validated
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "登录即可；类型按内容识别，只收白名单类型")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return success(fileService.createFile(file.getOriginalFilename(), file.getBytes()));
    }

    @GetMapping("/{configId}/get/**")
    @Operation(summary = "读取文件", description = "免登录；数据库与本地磁盘存储的文件经此输出")
    @ApiAccessLog(enable = false)
    public void getFileContent(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable("configId") Long configId) throws IOException {
        String path = StrUtil.subAfter(URLUtil.decode(request.getRequestURI()), "/get/", false);
        byte[] content = StrUtil.isBlank(path) || path.contains("..") || path.indexOf('\0') >= 0
                ? null : readContent(configId, path);
        if (content == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        FileTypeUtils.FileType type = FileTypeUtils.detect(content, path);
        String mimeType = type != null ? type.mimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        response.setContentType(mimeType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 路径含内容哈希，内容不会变，可以长期缓存
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                .builder(FileTypeUtils.isInline(mimeType) ? "inline" : "attachment")
                .filename(FileUtil.getName(path), StandardCharsets.UTF_8).build().toString());
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }

    /**
     * 路径来自 URL，配置不存在、路径不合法都当作文件不存在，不能让它们变成 500 或带业务码的 200
     */
    private byte[] readContent(Long configId, String path) {
        try {
            return fileService.getFileContent(configId, path);
        } catch (ServiceException e) {
            return null;
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件", description = "同一内容再无记录引用时，一并删除存储中的内容")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> deleteFile(@RequestParam("id") Long id) {
        fileService.deleteFile(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得文件分页")
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<PageResult<FileRespVO>> getFilePage(@Valid FilePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(fileService.getFilePage(pageReqVO), FileRespVO.class));
    }
}

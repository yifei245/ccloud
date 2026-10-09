package com.ccloud.module.infra.controller.app.file;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.infra.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 文件")
@RestController
@RequestMapping("/app-api/infra/file")
@Validated
public class AppFileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "登录即可；类型按内容识别，只收白名单类型")
    @ApiAccessLog(requestEnable = false)
    public CommonResult<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return success(fileService.createFile(file.getOriginalFilename(), file.getBytes()));
    }
}

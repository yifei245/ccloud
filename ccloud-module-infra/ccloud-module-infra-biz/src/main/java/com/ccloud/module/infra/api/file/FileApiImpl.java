package com.ccloud.module.infra.api.file;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.infra.api.file.dto.FileCreateReqDTO;
import com.ccloud.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public CommonResult<String> createFile(FileCreateReqDTO reqDTO) {
        return success(fileService.createFile(reqDTO.getName(), reqDTO.getContent()));
    }
}

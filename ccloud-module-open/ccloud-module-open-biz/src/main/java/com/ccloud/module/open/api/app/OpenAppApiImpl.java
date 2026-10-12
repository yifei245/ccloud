package com.ccloud.module.open.api.app;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.open.service.app.OpenAppService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class OpenAppApiImpl implements OpenAppApi {

    @Resource
    private OpenAppService openAppService;

    @Override
    public CommonResult<OpenAppSnapshotRespDTO> getAppSnapshot(String appKey) {
        return success(openAppService.getAppSnapshot(appKey));
    }
}

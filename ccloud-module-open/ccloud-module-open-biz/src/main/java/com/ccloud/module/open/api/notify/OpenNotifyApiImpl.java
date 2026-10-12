package com.ccloud.module.open.api.notify;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.open.api.notify.dto.OpenNotifySendReqDTO;
import com.ccloud.module.open.service.notify.OpenNotifyService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class OpenNotifyApiImpl implements OpenNotifyApi {

    @Resource
    private OpenNotifyService openNotifyService;

    @Override
    public CommonResult<Long> sendNotify(OpenNotifySendReqDTO reqDTO) {
        return success(openNotifyService.sendNotify(reqDTO.getAppId(), reqDTO.getEvent(), reqDTO.getData()));
    }
}

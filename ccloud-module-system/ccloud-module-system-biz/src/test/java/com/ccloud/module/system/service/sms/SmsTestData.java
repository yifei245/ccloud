package com.ccloud.module.system.service.sms;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.sms.vo.channel.SmsChannelSaveReqVO;
import com.ccloud.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import com.ccloud.module.system.enums.sms.SmsChannelEnum;
import com.ccloud.module.system.enums.sms.SmsTemplateTypeEnum;

/**
 * 短信测试数据
 */
public final class SmsTestData {

    private SmsTestData() {
    }

    public static SmsChannelSaveReqVO channelReqVO(String signature, CommonStatusEnum status) {
        SmsChannelSaveReqVO reqVO = new SmsChannelSaveReqVO();
        reqVO.setSignature(signature);
        reqVO.setCode(SmsChannelEnum.MOCK.getCode());
        reqVO.setStatus(status.getStatus());
        reqVO.setApiKey("key");
        reqVO.setApiSecret("secret");
        return reqVO;
    }

    public static SmsTemplateSaveReqVO templateReqVO(Long channelId, String code, String content) {
        SmsTemplateSaveReqVO reqVO = new SmsTemplateSaveReqVO();
        reqVO.setType(SmsTemplateTypeEnum.VERIFICATION_CODE.getType());
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCode(code);
        reqVO.setName("模板" + code);
        reqVO.setContent(content);
        reqVO.setApiTemplateId("api-" + code);
        reqVO.setChannelId(channelId);
        return reqVO;
    }
}

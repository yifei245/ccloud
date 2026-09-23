package com.ccloud.module.system.service.mail;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountSaveReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;

/**
 * 邮件测试数据
 */
public final class MailTestData {

    private MailTestData() {
    }

    public static MailAccountSaveReqVO accountReqVO(String mail) {
        MailAccountSaveReqVO reqVO = new MailAccountSaveReqVO();
        reqVO.setMail(mail);
        reqVO.setUsername(mail);
        reqVO.setPassword("smtp-password");
        reqVO.setHost("smtp.ccloud.local");
        reqVO.setPort(465);
        reqVO.setSslEnable(true);
        reqVO.setStarttlsEnable(false);
        return reqVO;
    }

    public static MailTemplateSaveReqVO templateReqVO(Long accountId, String code, String title, String content) {
        MailTemplateSaveReqVO reqVO = new MailTemplateSaveReqVO();
        reqVO.setName("模板" + code);
        reqVO.setCode(code);
        reqVO.setAccountId(accountId);
        reqVO.setNickname("ccloud");
        reqVO.setTitle(title);
        reqVO.setContent(content);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}

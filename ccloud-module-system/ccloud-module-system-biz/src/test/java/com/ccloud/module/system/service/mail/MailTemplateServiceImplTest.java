package com.ccloud.module.system.service.mail;

import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;
import com.ccloud.module.system.dal.mysql.mail.MailTemplateMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.mail.MailTestData.accountReqVO;
import static com.ccloud.module.system.service.mail.MailTestData.templateReqVO;
import static org.assertj.core.api.Assertions.assertThat;

@Import({MailTemplateServiceImpl.class, MailAccountServiceImpl.class})
class MailTemplateServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private MailTemplateService templateService;
    @Resource
    private MailAccountService accountService;
    @Resource
    private MailTemplateMapper templateMapper;

    private Long accountId;

    @BeforeEach
    void seed() {
        accountId = accountService.createMailAccount(accountReqVO("a@ccloud.local"));
    }

    @Test
    void createMailTemplate_成功时从标题与内容解析参数_去重保序() {
        Long id = templateService.createMailTemplate(templateReqVO(accountId, "t-reg", "欢迎 {name}",
                "<p>{name}，您的账号 {username} 已开通</p>"));
        assertThat(templateMapper.selectById(id).getParams()).containsExactly("name", "username");
    }

    @Test
    void createMailTemplate_邮箱账号不存在() {
        assertServiceException(() -> templateService.createMailTemplate(templateReqVO(-1L, "t1", "x", "y")),
                MAIL_ACCOUNT_NOT_EXISTS);
    }

    @Test
    void createMailTemplate_编码重复() {
        templateService.createMailTemplate(templateReqVO(accountId, "dup", "x", "y"));
        assertServiceException(() -> templateService.createMailTemplate(templateReqVO(accountId, "dup", "x", "y")),
                MAIL_TEMPLATE_CODE_EXISTS);
    }

    @Test
    void updateMailTemplate_成功与不存在与编码冲突() {
        templateService.createMailTemplate(templateReqVO(accountId, "taken", "x", "y"));
        Long id = templateService.createMailTemplate(templateReqVO(accountId, "self", "x", "y"));

        MailTemplateSaveReqVO reqVO = templateReqVO(accountId, "self", "新标题 {a}", "新内容");
        reqVO.setId(id);
        templateService.updateMailTemplate(reqVO);
        MailTemplateDO template = templateMapper.selectById(id);
        assertThat(template.getTitle()).isEqualTo("新标题 {a}");
        assertThat(template.getParams()).containsExactly("a");

        reqVO.setCode("taken");
        assertServiceException(() -> templateService.updateMailTemplate(reqVO), MAIL_TEMPLATE_CODE_EXISTS);
        reqVO.setId(-1L);
        assertServiceException(() -> templateService.updateMailTemplate(reqVO), MAIL_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void deleteMailTemplate_成功与不存在() {
        Long id = templateService.createMailTemplate(templateReqVO(accountId, "del", "x", "y"));
        templateService.deleteMailTemplate(id);
        assertThat(templateMapper.selectById(id)).isNull();
        assertServiceException(() -> templateService.deleteMailTemplate(-1L), MAIL_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void formatMailTemplateContent_参数值做HTML转义_防止注入链接() {
        String html = templateService.formatMailTemplateContent("<p>您好 {name}</p>",
                Map.of("name", "<a href=\"https://evil.com\">点我</a>"));
        assertThat(html).isEqualTo("<p>您好 &lt;a href=&quot;https://evil.com&quot;&gt;点我&lt;/a&gt;</p>");
    }

    @Test
    void formatMailTemplateTitle_标题为纯文本不转义() {
        assertThat(templateService.formatMailTemplateTitle("订单 {no} & 发货", Map.of("no", "A&B")))
                .isEqualTo("订单 A&B & 发货");
    }
}

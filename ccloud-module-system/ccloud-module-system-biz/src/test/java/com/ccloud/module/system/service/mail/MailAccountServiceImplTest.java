package com.ccloud.module.system.service.mail;

import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.dal.mysql.mail.MailAccountMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static com.ccloud.module.system.service.mail.MailTestData.accountReqVO;
import static com.ccloud.module.system.service.mail.MailTestData.templateReqVO;
import static org.assertj.core.api.Assertions.assertThat;

@Import({MailAccountServiceImpl.class, MailTemplateServiceImpl.class})
class MailAccountServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private MailAccountService accountService;
    @Resource
    private MailTemplateService templateService;
    @Resource
    private MailAccountMapper accountMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void createMailAccount_成功且密码加密存储() {
        Long id = accountService.createMailAccount(accountReqVO("a@ccloud.local"));

        assertThat(accountMapper.selectById(id).getPassword()).isEqualTo("smtp-password");
        String stored = jdbcTemplate.queryForObject("SELECT password FROM system_mail_account WHERE id = ?", String.class, id);
        assertThat(stored).isNotEqualTo("smtp-password");
    }

    @Test
    void createMailAccount_密码必填() {
        MailAccountSaveReqVO reqVO = accountReqVO("a@ccloud.local");
        reqVO.setPassword(null);
        assertServiceException(() -> accountService.createMailAccount(reqVO),
                com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST);
    }

    @Test
    void updateMailAccount_未填密码时保留原密码() {
        Long id = accountService.createMailAccount(accountReqVO("a@ccloud.local"));
        MailAccountSaveReqVO reqVO = accountReqVO("b@ccloud.local");
        reqVO.setId(id);
        reqVO.setPassword("");

        accountService.updateMailAccount(reqVO);

        MailAccountDO account = accountMapper.selectById(id);
        assertThat(account.getMail()).isEqualTo("b@ccloud.local");
        assertThat(account.getPassword()).isEqualTo("smtp-password");
    }

    @Test
    void updateMailAccount_填写密码时更新() {
        Long id = accountService.createMailAccount(accountReqVO("a@ccloud.local"));
        MailAccountSaveReqVO reqVO = accountReqVO("a@ccloud.local");
        reqVO.setId(id);
        reqVO.setPassword("new-password");

        accountService.updateMailAccount(reqVO);

        assertThat(accountMapper.selectById(id).getPassword()).isEqualTo("new-password");
    }

    @Test
    void updateMailAccount_不存在() {
        MailAccountSaveReqVO reqVO = accountReqVO("a@ccloud.local");
        reqVO.setId(-1L);
        assertServiceException(() -> accountService.updateMailAccount(reqVO), MAIL_ACCOUNT_NOT_EXISTS);
    }

    @Test
    void deleteMailAccount_成功与不存在() {
        Long id = accountService.createMailAccount(accountReqVO("a@ccloud.local"));
        accountService.deleteMailAccount(id);
        assertThat(accountMapper.selectById(id)).isNull();
        assertServiceException(() -> accountService.deleteMailAccount(-1L), MAIL_ACCOUNT_NOT_EXISTS);
    }

    @Test
    void deleteMailAccount_仍有模板时不允许删除() {
        Long id = accountService.createMailAccount(accountReqVO("a@ccloud.local"));
        templateService.createMailTemplate(templateReqVO(id, "t1", "标题", "内容"));
        assertServiceException(() -> accountService.deleteMailAccount(id), MAIL_ACCOUNT_RELATE_TEMPLATE_EXISTS);
    }
}

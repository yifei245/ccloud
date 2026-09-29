package com.ccloud.module.system.service.mail;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountPageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 邮箱账号 Service
 */
public interface MailAccountService {

    /**
     * 创建时密码必填
     */
    Long createMailAccount(@Valid MailAccountSaveReqVO createReqVO);

    /**
     * 密码为空时保留原密码：管理页面不回显密码
     */
    void updateMailAccount(@Valid MailAccountSaveReqVO updateReqVO);

    /**
     * 仍有模板使用时不允许删除
     */
    void deleteMailAccount(Long id);

    MailAccountDO getMailAccount(Long id);

    List<MailAccountDO> getMailAccountList();

    PageResult<MailAccountDO> getMailAccountPage(MailAccountPageReqVO pageReqVO);
}

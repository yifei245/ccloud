package com.ccloud.module.system.service.mail;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountPageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import com.ccloud.module.system.dal.mysql.mail.MailAccountMapper;
import com.ccloud.module.system.dal.mysql.mail.MailTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.system.enums.ErrorCodeConstants.MAIL_ACCOUNT_NOT_EXISTS;
import static com.ccloud.module.system.enums.ErrorCodeConstants.MAIL_ACCOUNT_RELATE_TEMPLATE_EXISTS;

@Service
@Validated
public class MailAccountServiceImpl implements MailAccountService {

    @Resource
    private MailAccountMapper mailAccountMapper;
    @Resource
    private MailTemplateMapper mailTemplateMapper;

    @Override
    public Long createMailAccount(MailAccountSaveReqVO createReqVO) {
        if (StrUtil.isEmpty(createReqVO.getPassword())) {
            throw invalidParamException("密码不能为空");
        }
        MailAccountDO account = BeanUtils.toBean(createReqVO, MailAccountDO.class);
        mailAccountMapper.insert(account);
        return account.getId();
    }

    @Override
    public void updateMailAccount(MailAccountSaveReqVO updateReqVO) {
        validateMailAccountExists(updateReqVO.getId());
        MailAccountDO updateObj = BeanUtils.toBean(updateReqVO, MailAccountDO.class);
        if (StrUtil.isEmpty(updateObj.getPassword())) {
            // 为 null 的字段不参与更新
            updateObj.setPassword(null);
        }
        mailAccountMapper.updateById(updateObj);
    }

    @Override
    public void deleteMailAccount(Long id) {
        validateMailAccountExists(id);
        if (mailTemplateMapper.selectCountByAccountId(id) > 0) {
            throw exception(MAIL_ACCOUNT_RELATE_TEMPLATE_EXISTS);
        }
        mailAccountMapper.deleteById(id);
    }

    private void validateMailAccountExists(Long id) {
        if (mailAccountMapper.selectById(id) == null) {
            throw exception(MAIL_ACCOUNT_NOT_EXISTS);
        }
    }

    @Override
    public MailAccountDO getMailAccount(Long id) {
        return mailAccountMapper.selectById(id);
    }

    @Override
    public List<MailAccountDO> getMailAccountList() {
        return mailAccountMapper.selectList();
    }

    @Override
    public PageResult<MailAccountDO> getMailAccountPage(MailAccountPageReqVO pageReqVO) {
        return mailAccountMapper.selectPage(pageReqVO);
    }
}

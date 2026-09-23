package com.ccloud.module.system.service.mail;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;
import com.ccloud.module.system.dal.mysql.mail.MailTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.regex.Pattern;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MailTemplateServiceImpl implements MailTemplateService {

    /**
     * 模板参数占位符，形如 {name}
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(\\w+)}");

    @Resource
    private MailTemplateMapper mailTemplateMapper;
    @Resource
    private MailAccountService mailAccountService;

    @Override
    public Long createMailTemplate(MailTemplateSaveReqVO createReqVO) {
        validateMailAccountExists(createReqVO.getAccountId());
        validateCodeUnique(null, createReqVO.getCode());
        MailTemplateDO template = BeanUtils.toBean(createReqVO, MailTemplateDO.class)
                .setParams(parseParams(createReqVO.getTitle(), createReqVO.getContent()));
        mailTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateMailTemplate(MailTemplateSaveReqVO updateReqVO) {
        validateMailTemplateExists(updateReqVO.getId());
        validateMailAccountExists(updateReqVO.getAccountId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        mailTemplateMapper.updateById(BeanUtils.toBean(updateReqVO, MailTemplateDO.class)
                .setParams(parseParams(updateReqVO.getTitle(), updateReqVO.getContent())));
    }

    @Override
    public void deleteMailTemplate(Long id) {
        validateMailTemplateExists(id);
        mailTemplateMapper.deleteById(id);
    }

    private void validateMailTemplateExists(Long id) {
        if (mailTemplateMapper.selectById(id) == null) {
            throw exception(MAIL_TEMPLATE_NOT_EXISTS);
        }
    }

    private void validateMailAccountExists(Long accountId) {
        if (mailAccountService.getMailAccount(accountId) == null) {
            throw exception(MAIL_ACCOUNT_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        MailTemplateDO template = mailTemplateMapper.selectByCode(code);
        if (template != null && !Objects.equals(template.getId(), id)) {
            throw exception(MAIL_TEMPLATE_CODE_EXISTS, code);
        }
    }

    /**
     * 标题在前、内容在后，按首次出现的顺序去重
     */
    private static List<String> parseParams(String title, String content) {
        Set<String> params = new LinkedHashSet<>(ReUtil.findAllGroup1(PARAM_PATTERN, title));
        params.addAll(ReUtil.findAllGroup1(PARAM_PATTERN, content));
        return new ArrayList<>(params);
    }

    @Override
    public MailTemplateDO getMailTemplate(Long id) {
        return mailTemplateMapper.selectById(id);
    }

    @Override
    public MailTemplateDO getMailTemplateByCode(String code) {
        return mailTemplateMapper.selectByCode(code);
    }

    @Override
    public PageResult<MailTemplateDO> getMailTemplatePage(MailTemplatePageReqVO pageReqVO) {
        return mailTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public String formatMailTemplateTitle(String title, Map<String, Object> params) {
        return StrUtil.format(title, params);
    }

    @Override
    public String formatMailTemplateContent(String content, Map<String, Object> params) {
        Map<String, Object> escaped = new HashMap<>();
        params.forEach((key, value) -> escaped.put(key, value == null ? null : HtmlUtil.escape(String.valueOf(value))));
        return StrUtil.format(content, escaped);
    }
}

package com.ccloud.module.system.service.notify;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplatePageReqVO;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateSaveReqVO;
import com.ccloud.module.system.dal.dataobject.notify.NotifyTemplateDO;
import com.ccloud.module.system.dal.mysql.notify.NotifyTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_CODE_DUPLICATE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_NOT_EXISTS;

@Service
@Validated
public class NotifyTemplateServiceImpl implements NotifyTemplateService {

    /**
     * 模板参数占位符，形如 {name}
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(\\w+)}");

    @Resource
    private NotifyTemplateMapper notifyTemplateMapper;

    @Override
    public Long createNotifyTemplate(NotifyTemplateSaveReqVO createReqVO) {
        validateCodeUnique(null, createReqVO.getCode());
        NotifyTemplateDO template = BeanUtils.toBean(createReqVO, NotifyTemplateDO.class)
                .setParams(parseParams(createReqVO.getContent()));
        notifyTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateNotifyTemplate(NotifyTemplateSaveReqVO updateReqVO) {
        validateNotifyTemplateExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        notifyTemplateMapper.updateById(BeanUtils.toBean(updateReqVO, NotifyTemplateDO.class)
                .setParams(parseParams(updateReqVO.getContent())));
    }

    @Override
    public void deleteNotifyTemplate(Long id) {
        validateNotifyTemplateExists(id);
        notifyTemplateMapper.deleteById(id);
    }

    private void validateNotifyTemplateExists(Long id) {
        if (notifyTemplateMapper.selectById(id) == null) {
            throw exception(NOTIFY_TEMPLATE_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        NotifyTemplateDO template = notifyTemplateMapper.selectByCode(code);
        if (template != null && !Objects.equals(template.getId(), id)) {
            throw exception(NOTIFY_TEMPLATE_CODE_DUPLICATE, code);
        }
    }

    private static java.util.List<String> parseParams(String content) {
        return new ArrayList<>(new LinkedHashSet<>(ReUtil.findAllGroup1(PARAM_PATTERN, content)));
    }

    @Override
    public NotifyTemplateDO getNotifyTemplate(Long id) {
        return notifyTemplateMapper.selectById(id);
    }

    @Override
    public NotifyTemplateDO getNotifyTemplateByCode(String code) {
        return notifyTemplateMapper.selectByCode(code);
    }

    @Override
    public PageResult<NotifyTemplateDO> getNotifyTemplatePage(NotifyTemplatePageReqVO pageReqVO) {
        return notifyTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public String formatNotifyTemplateContent(String content, Map<String, Object> params) {
        return StrUtil.format(content, params);
    }
}

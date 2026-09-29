package com.ccloud.module.system.service.i18n;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.i18n.config.I18nProperties;
import com.ccloud.framework.i18n.core.I18nMessageRefresher;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessagePageReqVO;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessageSaveReqVO;
import com.ccloud.module.system.dal.dataobject.i18n.I18nMessageDO;
import com.ccloud.module.system.dal.mysql.i18n.I18nMessageMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.I18N_MESSAGE_MAP;
import static com.ccloud.module.system.enums.ErrorCodeConstants.I18N_MESSAGE_DUPLICATE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.I18N_MESSAGE_NOT_EXISTS;

@Service
@Validated
public class I18nMessageServiceImpl implements I18nMessageService {

    @Resource
    private I18nMessageMapper i18nMessageMapper;
    @Resource
    private I18nProperties i18nProperties;
    @Resource
    private CacheManager cacheManager;
    @Resource
    private ObjectProvider<I18nMessageRefresher> refresherProvider;

    @Override
    public Long createI18nMessage(I18nMessageSaveReqVO createReqVO) {
        validateLocale(createReqVO.getLocale());
        validateCodeUnique(null, createReqVO.getLocale(), createReqVO.getCode());
        I18nMessageDO message = BeanUtils.toBean(createReqVO, I18nMessageDO.class).setSource(I18nMessageDO.SOURCE_SYSTEM);
        i18nMessageMapper.insert(message);
        afterChange();
        return message.getId();
    }

    @Override
    public void updateI18nMessage(I18nMessageSaveReqVO updateReqVO) {
        validateI18nMessageExists(updateReqVO.getId());
        validateLocale(updateReqVO.getLocale());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getLocale(), updateReqVO.getCode());
        i18nMessageMapper.updateById(BeanUtils.toBean(updateReqVO, I18nMessageDO.class));
        afterChange();
    }

    @Override
    public void deleteI18nMessage(Long id) {
        validateI18nMessageExists(id);
        i18nMessageMapper.deleteById(id);
        afterChange();
    }

    /**
     * 先清缓存再通知刷新：顺序反了，其他服务可能在缓存清除前重新加载，拿到的仍是旧文案
     */
    private void afterChange() {
        Cache cache = cacheManager.getCache(I18N_MESSAGE_MAP);
        if (cache != null) {
            cache.clear();
        }
        I18nMessageRefresher refresher = refresherProvider.getIfAvailable();
        if (refresher != null) {
            refresher.publishRefresh();
        }
    }

    private void validateI18nMessageExists(Long id) {
        if (i18nMessageMapper.selectById(id) == null) {
            throw exception(I18N_MESSAGE_NOT_EXISTS);
        }
    }

    /**
     * 不支持的语言永远不会被加载，写入只会产生无效数据
     */
    private void validateLocale(String locale) {
        if (!i18nProperties.getSupportedLocales().contains(locale)) {
            throw invalidParamException("不支持的语言({})，可选：{}", locale, i18nProperties.getSupportedLocales());
        }
    }

    private void validateCodeUnique(Long id, String locale, String code) {
        I18nMessageDO message = i18nMessageMapper.selectByLocaleAndCode(locale, code);
        if (message != null && !Objects.equals(message.getId(), id)) {
            throw exception(I18N_MESSAGE_DUPLICATE, locale, code);
        }
    }

    @Override
    public I18nMessageDO getI18nMessage(Long id) {
        return i18nMessageMapper.selectById(id);
    }

    @Override
    public PageResult<I18nMessageDO> getI18nMessagePage(I18nMessagePageReqVO pageReqVO) {
        return i18nMessageMapper.selectPage(pageReqVO);
    }

    @Override
    @Cacheable(value = I18N_MESSAGE_MAP, key = "#locale")
    public Map<String, String> getMessages(String locale) {
        Map<String, String> messages = new LinkedHashMap<>();
        i18nMessageMapper.selectListByLocale(locale).forEach(message -> messages.put(message.getCode(), message.getContent()));
        return messages;
    }
}

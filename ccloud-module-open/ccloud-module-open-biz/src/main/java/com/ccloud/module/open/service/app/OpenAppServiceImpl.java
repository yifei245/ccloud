package com.ccloud.module.open.service.app;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.net.IpWhitelistUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppCreateRespVO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppPageReqVO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppSaveReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppApiDO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import com.ccloud.module.open.dal.mysql.app.OpenAppApiMapper;
import com.ccloud.module.open.dal.mysql.app.OpenAppMapper;
import com.ccloud.module.open.service.api.OpenApiService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.open.enums.ErrorCodeConstants.*;

@Service
@Validated
public class OpenAppServiceImpl implements OpenAppService {

    @Resource
    private OpenAppMapper openAppMapper;
    @Resource
    private OpenAppApiMapper openAppApiMapper;
    @Resource
    private OpenApiService openApiService;
    @Resource
    private OpenAppCacheInvalidator invalidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenAppCreateRespVO createApp(OpenAppSaveReqVO reqVO) {
        validateFields(reqVO);
        OpenAppDO app = BeanUtils.toBean(reqVO, OpenAppDO.class).setId(null)
                .setAppKey(generateUniqueAppKey())
                .setAppSecret(OpenAppSecretUtils.generateSecret());
        openAppMapper.insert(app);
        return new OpenAppCreateRespVO().setId(app.getId()).setAppKey(app.getAppKey()).setAppSecret(app.getAppSecret());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApp(OpenAppSaveReqVO reqVO) {
        OpenAppDO old = validateAppExists(reqVO.getId());
        validateFields(reqVO);
        OpenAppDO update = BeanUtils.toBean(reqVO, OpenAppDO.class);
        update.setAppKey(null).setAppSecret(null);   // 不可修改；null 字段不更新
        openAppMapper.updateById(update);
        // 允许清空的字段：NOT_NULL 更新策略下 null 不会写入，需显式置空
        openAppMapper.update(null, new LambdaUpdateWrapper<OpenAppDO>().eq(OpenAppDO::getId, reqVO.getId())
                .set(OpenAppDO::getIpWhitelist, StrUtil.emptyToNull(reqVO.getIpWhitelist()))
                .set(OpenAppDO::getExpireTime, reqVO.getExpireTime())
                .set(OpenAppDO::getNotifyUrl, StrUtil.emptyToNull(reqVO.getNotifyUrl())));
        invalidator.publish(old.getAppKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApp(Long id) {
        OpenAppDO app = validateAppExists(id);
        openAppApiMapper.deleteByAppId(id);
        openAppMapper.deleteById(id);
        invalidator.publish(app.getAppKey());
    }

    @Override
    public OpenAppDO getApp(Long id) {
        return openAppMapper.selectById(id);
    }

    @Override
    public OpenAppDO getAppIgnoreTenant(Long id) {
        return TenantUtils.executeIgnore(() -> openAppMapper.selectById(id));
    }

    @Override
    public PageResult<OpenAppDO> getAppPage(OpenAppPageReqVO reqVO) {
        return openAppMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetAppSecret(Long id) {
        OpenAppDO app = validateAppExists(id);
        String secret = OpenAppSecretUtils.generateSecret();
        openAppMapper.updateById(new OpenAppDO().setId(id).setAppSecret(secret));
        invalidator.publish(app.getAppKey());
        return secret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignApis(Long appId, Set<Long> apiIds) {
        OpenAppDO app = validateAppExists(appId);
        Set<Long> enabled = CollectionUtils.convertSet(openApiService.getEnabledApiList(), OpenApiDO::getId);
        apiIds.stream().filter(id -> !enabled.contains(id)).findFirst()
                .ifPresent(id -> { throw exception(OPEN_APP_API_NOT_ENABLED, id); });
        openAppApiMapper.deleteByAppId(appId);
        apiIds.forEach(apiId -> openAppApiMapper.insert(new OpenAppApiDO().setAppId(appId).setApiId(apiId)));
        invalidator.publish(app.getAppKey());
    }

    @Override
    public Set<Long> getAppApiIds(Long appId) {
        return CollectionUtils.convertSet(openAppApiMapper.selectListByAppId(appId), OpenAppApiDO::getApiId);
    }

    /**
     * 网关调用时没有租户上下文：按 appKey 跨租户查询
     */
    @Override
    public OpenAppSnapshotRespDTO getAppSnapshot(String appKey) {
        return TenantUtils.executeIgnore(() -> {
            OpenAppDO app = openAppMapper.selectByAppKey(appKey);
            if (app == null) {
                return null;
            }
            Set<Long> apiIds = CollectionUtils.convertSet(openAppApiMapper.selectListByAppId(app.getId()), OpenAppApiDO::getApiId);
            List<OpenAppSnapshotRespDTO.Api> apis = openApiService.getEnabledApiList().stream()
                    .filter(api -> apiIds.contains(api.getId()))
                    .map(api -> new OpenAppSnapshotRespDTO.Api().setMethod(api.getMethod()).setPath(api.getPath()))
                    .toList();
            return BeanUtils.toBean(app, OpenAppSnapshotRespDTO.class).setApis(apis);
        });
    }

    private OpenAppDO validateAppExists(Long id) {
        OpenAppDO app = id == null ? null : openAppMapper.selectById(id);
        if (app == null) {
            throw exception(OPEN_APP_NOT_EXISTS);
        }
        return app;
    }

    private void validateFields(OpenAppSaveReqVO reqVO) {
        try {
            IpWhitelistUtils.validate(reqVO.getIpWhitelist());
        } catch (IllegalArgumentException e) {
            throw exception(OPEN_APP_IP_WHITELIST_INVALID, e.getMessage());
        }
        String url = reqVO.getNotifyUrl();
        if (StrUtil.isNotBlank(url)) {
            URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw exception(OPEN_APP_NOTIFY_URL_INVALID);
            }
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || StrUtil.isBlank(uri.getHost())) {
                throw exception(OPEN_APP_NOTIFY_URL_INVALID);
            }
        }
    }

    private String generateUniqueAppKey() {
        for (int i = 0; i < 5; i++) {
            String key = OpenAppSecretUtils.generateAppKey();
            if (TenantUtils.executeIgnore(() -> openAppMapper.selectByAppKey(key)) == null) {
                return key;
            }
        }
        throw new IllegalStateException("生成应用标识失败，请重试");
    }
}

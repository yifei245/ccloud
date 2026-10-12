package com.ccloud.module.open.service.api;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiPageReqVO;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiSaveReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import com.ccloud.module.open.dal.mysql.api.OpenApiMapper;
import com.ccloud.module.open.dal.mysql.app.OpenAppApiMapper;
import com.ccloud.module.open.service.app.OpenAppCacheInvalidator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.open.enums.ErrorCodeConstants.*;

@Service
@Validated
public class OpenApiServiceImpl implements OpenApiService {

    private static final Pattern PATH = Pattern.compile("/open-api/[A-Za-z0-9_\\-/.]+");

    @Resource
    private OpenApiMapper openApiMapper;
    @Resource
    private OpenAppApiMapper openAppApiMapper;
    @Resource
    private OpenAppCacheInvalidator invalidator;
    @Resource
    private SystemTenantValidator systemTenantValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApi(OpenApiSaveReqVO reqVO) {
        systemTenantValidator.validate(ONLY_SYSTEM_TENANT);
        OpenApiDO api = normalize(BeanUtils.toBean(reqVO, OpenApiDO.class).setId(null));
        validateUnique(null, api);
        openApiMapper.insert(api);
        invalidator.publishAll();
        return api.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApi(OpenApiSaveReqVO reqVO) {
        systemTenantValidator.validate(ONLY_SYSTEM_TENANT);
        validateExists(reqVO.getId());
        OpenApiDO api = normalize(BeanUtils.toBean(reqVO, OpenApiDO.class));
        validateUnique(api.getId(), api);
        openApiMapper.updateById(api);
        invalidator.publishAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApi(Long id) {
        systemTenantValidator.validate(ONLY_SYSTEM_TENANT);
        validateExists(id);
        openApiMapper.deleteById(id);
        // 授权行属于各租户的应用：忽略租户，一并删除
        TenantUtils.executeIgnore(() -> openAppApiMapper.deleteByApiId(id));
        invalidator.publishAll();
    }

    @Override
    public OpenApiDO getApi(Long id) {
        return openApiMapper.selectById(id);
    }

    @Override
    public PageResult<OpenApiDO> getApiPage(OpenApiPageReqVO reqVO) {
        return openApiMapper.selectPage(reqVO);
    }

    @Override
    public List<OpenApiDO> getEnabledApiList() {
        return openApiMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public OpenApiDO getApiByMethodAndPath(String method, String path) {
        return openApiMapper.selectByMethodAndPath(method, path);
    }

    private void validateExists(Long id) {
        if (id == null || openApiMapper.selectById(id) == null) {
            throw exception(OPEN_API_NOT_EXISTS);
        }
    }

    /**
     * 路径只允许字母数字与 _ - / .，且不含 ..：网关按原始路径精确匹配，查询串、通配符、编码字符都会让授权判断与实际路由不一致
     */
    private static OpenApiDO normalize(OpenApiDO api) {
        api.setMethod(api.getMethod().toUpperCase(Locale.ROOT));
        String path = api.getPath();
        if (!PATH.matcher(path).matches() || path.contains("..") || path.contains("//")) {
            throw exception(OPEN_API_PATH_INVALID);
        }
        return api;
    }

    private void validateUnique(Long id, OpenApiDO api) {
        OpenApiDO byCode = openApiMapper.selectByCode(api.getCode());
        if (byCode != null && !byCode.getId().equals(id)) {
            throw exception(OPEN_API_CODE_EXISTS);
        }
        OpenApiDO byPath = openApiMapper.selectByMethodAndPath(api.getMethod(), api.getPath());
        if (byPath != null && !byPath.getId().equals(id)) {
            throw exception(OPEN_API_METHOD_PATH_EXISTS);
        }
    }
}

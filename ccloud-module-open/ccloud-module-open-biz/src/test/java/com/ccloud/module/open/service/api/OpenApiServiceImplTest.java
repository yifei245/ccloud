package com.ccloud.module.open.service.api;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.service.SystemTenantValidator;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiSaveReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppApiDO;
import com.ccloud.module.open.dal.mysql.api.OpenApiMapper;
import com.ccloud.module.open.dal.mysql.app.OpenAppApiMapper;
import com.ccloud.module.open.service.app.OpenAppCacheInvalidator;
import com.ccloud.module.open.test.BaseOpenDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.open.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import(OpenApiServiceImpl.class)
class OpenApiServiceImplTest extends BaseOpenDbAndRedisUnitTest {

    @Resource
    private OpenApiService openApiService;
    @Resource
    private OpenApiMapper openApiMapper;
    @Resource
    private OpenAppApiMapper openAppApiMapper;
    @MockitoBean
    private OpenAppCacheInvalidator invalidator;
    /**
     * 单测基类不装配系统租户校验，这里以桩模拟：租户 1 是系统租户，其余租户被拒绝
     */
    @MockitoBean
    private SystemTenantValidator systemTenantValidator;

    @Test
    void 创建_路径方法编码校验与唯一() {
        Long id = openApiService.createApi(req("webtest.a", "GET", "/open-api/webtest/a"));
        assertThat(openApiService.getApi(id).getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertServiceException(() -> openApiService.createApi(req("webtest.a", "POST", "/open-api/webtest/b")), OPEN_API_CODE_EXISTS);
        assertServiceException(() -> openApiService.createApi(req("webtest.b", "GET", "/open-api/webtest/a")), OPEN_API_METHOD_PATH_EXISTS);
        for (String bad : List.of("/admin-api/x", "/open-api/a?x=1", "/open-api/../admin-api/x", "/open-api/a/*", "/open-api/a/{id}", "open-api/a")) {
            assertServiceException(() -> openApiService.createApi(req("webtest.c", "GET", bad)), OPEN_API_PATH_INVALID);
        }
    }

    @Test
    void 方法统一为大写_只接受GET_POST_PUT_DELETE() {
        Long id = openApiService.createApi(req("webtest.d", "post", "/open-api/webtest/d"));
        assertThat(openApiService.getApi(id).getMethod()).isEqualTo("POST");
        // PATCH 由 VO 的 @Pattern 拦截，见控制器测试
    }

    @Test
    void 修改_删除_发布全部失效_删除时级联删除所有租户的授权() {
        Long id = openApiService.createApi(req("webtest.e", "GET", "/open-api/webtest/e"));
        openAppApiMapper.insert(new OpenAppApiDO().setAppId(99L).setApiId(id));
        TenantUtils.execute(2L, () -> openAppApiMapper.insert(new OpenAppApiDO().setAppId(98L).setApiId(id)));
        assertThat(TenantUtils.executeIgnore(() -> openAppApiMapper.selectCount(OpenAppApiDO::getApiId, id))).isEqualTo(2L);
        clearInvocations(invalidator);

        OpenApiSaveReqVO update = req("webtest.e", "GET", "/open-api/webtest/e2").setId(id);
        openApiService.updateApi(update);
        verify(invalidator).publishAll();
        assertThat(openApiService.getApi(id).getPath()).isEqualTo("/open-api/webtest/e2");

        openApiService.deleteApi(id);
        assertThat(openApiService.getApi(id)).isNull();
        assertThat(TenantUtils.executeIgnore(() -> openAppApiMapper.selectCount(OpenAppApiDO::getApiId, id))).isZero();
        verify(invalidator, times(2)).publishAll();
    }

    @Test
    void 修改_删除不存在的接口报错_改成他人编码或路径报错() {
        assertServiceException(() -> openApiService.updateApi(req("webtest.x", "GET", "/open-api/webtest/x").setId(-1L)),
                OPEN_API_NOT_EXISTS);
        assertServiceException(() -> openApiService.deleteApi(-1L), OPEN_API_NOT_EXISTS);

        Long a = openApiService.createApi(req("webtest.g", "GET", "/open-api/webtest/g"));
        openApiService.createApi(req("webtest.h", "GET", "/open-api/webtest/h"));
        assertServiceException(() -> openApiService.updateApi(req("webtest.h", "GET", "/open-api/webtest/g").setId(a)),
                OPEN_API_CODE_EXISTS);
        assertServiceException(() -> openApiService.updateApi(req("webtest.g", "GET", "/open-api/webtest/h").setId(a)),
                OPEN_API_METHOD_PATH_EXISTS);
        // 不改唯一字段时自己不算冲突
        openApiService.updateApi(req("webtest.g", "GET", "/open-api/webtest/g").setName("改名").setId(a));
        assertThat(openApiService.getApi(a).getName()).isEqualTo("改名");
    }

    @Test
    void 非系统租户不能增删改() {
        Long id = openApiService.createApi(req("webtest.f", "GET", "/open-api/webtest/f"));
        clearInvocations(invalidator);
        doThrow(exception(ONLY_SYSTEM_TENANT)).when(systemTenantValidator).validate(ONLY_SYSTEM_TENANT);
        TenantContextHolder.setTenantId(2L);

        assertServiceException(() -> openApiService.createApi(req("webtest.f2", "GET", "/open-api/webtest/f2")), ONLY_SYSTEM_TENANT);
        assertServiceException(() -> openApiService.updateApi(req("webtest.f", "GET", "/open-api/webtest/f3").setId(id)), ONLY_SYSTEM_TENANT);
        assertServiceException(() -> openApiService.deleteApi(id), ONLY_SYSTEM_TENANT);
        assertThat(openApiMapper.selectById(id).getPath()).isEqualTo("/open-api/webtest/f");
        verify(invalidator, never()).publishAll();
    }

    @Test
    void 按方法与路径查找_只返回开启的接口列表() {
        Long on = openApiService.createApi(req("webtest.on", "GET", "/open-api/webtest/on"));
        Long off = openApiService.createApi(req("webtest.off", "GET", "/open-api/webtest/off")
                .setStatus(CommonStatusEnum.DISABLE.getStatus()));

        assertThat(openApiService.getApiByMethodAndPath("GET", "/open-api/webtest/on").getId()).isEqualTo(on);
        assertThat(openApiService.getApiByMethodAndPath("POST", "/open-api/webtest/on")).isNull();
        assertThat(openApiService.getApiByMethodAndPath("GET", "/open-api/webtest/on/")).isNull();
        // 关闭的接口仍可按方法与路径查到，但不在开启列表中
        assertThat(openApiService.getApiByMethodAndPath("GET", "/open-api/webtest/off").getId()).isEqualTo(off);
        assertThat(openApiService.getEnabledApiList()).extracting(OpenApiDO::getId).contains(on).doesNotContain(off);
    }

    private static OpenApiSaveReqVO req(String code, String method, String path) {
        return new OpenApiSaveReqVO().setName("Web测试").setCode(code).setMethod(method).setPath(path)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }
}

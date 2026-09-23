package com.ccloud.framework.operatelog;

import com.ccloud.framework.apilog.core.enums.OperateTypeEnum;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.operatelog.core.annotation.OperateLog;
import com.ccloud.framework.operatelog.core.aop.OperateLogAspect;
import com.ccloud.framework.operatelog.core.service.OperateLogFrameworkService;
import com.ccloud.framework.operatelog.core.service.OperateLogRecord;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperateLogAspectTest {

    private final List<OperateLogRecord> records = new ArrayList<>();
    private DemoController controller;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin-api/system/user/update");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        WebFrameworkUtils.setLoginUserId(request, 7L);
        WebFrameworkUtils.setLoginUserType(request, 2);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        controller = proxy(records::add);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 注解生效时写入一条记录() {
        String result = controller.update(new UserReq(42L, "张三", "secret-pwd"));

        assertThat(result).isEqualTo("ok");
        assertThat(records).hasSize(1);
        OperateLogRecord record = records.get(0);
        assertThat(record.getType()).isEqualTo("用户管理");
        assertThat(record.getSubType()).isEqualTo("修改用户");
        assertThat(record.getBizId()).as("按 SpEL 从参数取业务编号").isEqualTo(42L);
        assertThat(record.getSuccess()).isTrue();
        assertThat(record.getUserId()).isEqualTo(7L);
        assertThat(record.getUserType()).isEqualTo(2);
        assertThat(record.getRequestMethod()).isEqualTo("PUT");
        assertThat(record.getRequestUrl()).isEqualTo("/admin-api/system/user/update");
        assertThat(record.getUserIp()).isEqualTo("10.0.0.1");
        assertThat(record.getUserAgent()).isEqualTo("JUnit");

        Map<?, ?> extra = JsonUtils.parseObject(record.getExtra(), Map.class);
        assertThat(extra.get("operateType")).isEqualTo(OperateTypeEnum.UPDATE.getType());
        assertThat(extra.get("result")).isEqualTo("ok");
        assertThat(extra.get("duration")).isNotNull();
        assertThat(record.getExtra()).contains("张三");
    }

    @Test
    void 敏感字段在记录前被脱敏() {
        controller.update(new UserReq(42L, "张三", "secret-pwd"));
        controller.login(Map.of("username", "admin", "Password", "p@ss", "nested", Map.of("accessToken", "t-123")));

        assertThat(records).extracting(OperateLogRecord::getExtra)
                .noneMatch(extra -> extra.contains("secret-pwd") || extra.contains("p@ss") || extra.contains("t-123"))
                .allMatch(extra -> extra.contains("******"));
    }

    @Test
    void 方法抛异常时也记录且标记失败_异常照常抛出() {
        assertThatThrownBy(() -> controller.delete(9L)).isInstanceOf(IllegalStateException.class).hasMessage("有下级部门");

        assertThat(records).hasSize(1);
        OperateLogRecord record = records.get(0);
        assertThat(record.getSuccess()).isFalse();
        assertThat(record.getBizId()).isEqualTo(9L);
        assertThat(record.getAction()).contains("有下级部门");
    }

    @Test
    void 未声明业务编号时记为0() {
        controller.login(Map.of("username", "admin"));
        assertThat(records.get(0).getBizId()).isZero();
    }

    @Test
    void 记录日志失败不影响业务() {
        DemoController broken = proxy(record -> {
            throw new IllegalStateException("日志库不可用");
        });
        assertThat(broken.update(new UserReq(1L, "a", "b"))).isEqualTo("ok");
    }

    @Test
    void 没有请求上下文时也能记录_如定时任务调用() {
        RequestContextHolder.resetRequestAttributes();
        controller.update(new UserReq(1L, "a", "b"));
        assertThat(records.get(0).getUserId()).isZero();
        assertThat(records.get(0).getRequestUrl()).isEmpty();
    }

    private static DemoController proxy(OperateLogFrameworkService service) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoController());
        factory.addAspect(new OperateLogAspect(service));
        return factory.getProxy();
    }

    @Data
    @AllArgsConstructor
    public static class UserReq {
        private Long id;
        private String nickname;
        private String password;
    }

    public static class DemoController {

        @OperateLog(type = "用户管理", subType = "修改用户", bizId = "#reqVO.id", operateType = OperateTypeEnum.UPDATE)
        public String update(UserReq reqVO) {
            return "ok";
        }

        @OperateLog(type = "部门管理", subType = "删除部门", bizId = "#id", operateType = OperateTypeEnum.DELETE)
        public void delete(Long id) {
            throw new IllegalStateException("有下级部门");
        }

        @OperateLog(type = "认证", subType = "登录")
        public String login(Map<String, Object> body) {
            return "ok";
        }
    }
}

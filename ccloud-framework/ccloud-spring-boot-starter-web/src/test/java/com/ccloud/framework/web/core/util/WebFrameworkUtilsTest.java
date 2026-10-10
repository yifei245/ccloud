package com.ccloud.framework.web.core.util;

import com.ccloud.framework.common.enums.UserTypeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebFrameworkUtilsTest {

    @Test
    void 按路径前缀推断用户类型() {
        assertThat(WebFrameworkUtils.getUserTypeByUrl("/admin-api/system/user/page"))
                .isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(WebFrameworkUtils.getUserTypeByUrl("/app-api/member/user/get"))
                .isEqualTo(UserTypeEnum.MEMBER.getValue());
        assertThat(WebFrameworkUtils.getUserTypeByUrl("/open-api/member/user/get"))
                .isEqualTo(UserTypeEnum.OPEN_APP.getValue());
        assertThat(WebFrameworkUtils.getUserTypeByUrl("/rpc-api/system/tenant/valid")).isNull();
        assertThat(WebFrameworkUtils.getUserTypeByUrl(null)).isNull();
    }
}

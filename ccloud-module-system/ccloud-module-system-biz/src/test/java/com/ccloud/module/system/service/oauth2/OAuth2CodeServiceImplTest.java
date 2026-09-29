package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_EXPIRE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

@Import(OAuth2CodeServiceImpl.class)
class OAuth2CodeServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private OAuth2CodeService codeService;
    @Resource
    private OAuth2CodeMapper codeMapper;

    @Test
    void createAuthorizationCode_有效期5分钟() {
        OAuth2CodeDO code = codeService.createAuthorizationCode(1L, UserTypeEnum.ADMIN.getValue(), "c",
                List.of("user.read"), "http://localhost:8080/cb", "xyz");

        assertThat(code.getCode()).hasSize(32);
        assertThat(Duration.between(LocalDateTime.now(), code.getExpiresTime()).getSeconds()).isBetween(290L, 300L);
        assertThat(codeMapper.selectByCode(code.getCode()).getState()).isEqualTo("xyz");
    }

    @Test
    void code只能被消费一次_第二次应抛异常() {
        OAuth2CodeDO code = codeService.createAuthorizationCode(1L, UserTypeEnum.ADMIN.getValue(), "c",
                List.of(), null, null);

        OAuth2CodeDO consumed = codeService.consumeAuthorizationCode(code.getCode());
        assertThat(consumed.getUserId()).isEqualTo(1L);

        assertServiceException(() -> codeService.consumeAuthorizationCode(code.getCode()), OAUTH2_CODE_NOT_EXISTS);
    }

    @Test
    void code超过5分钟应视为过期() {
        OAuth2CodeDO code = codeService.createAuthorizationCode(1L, UserTypeEnum.ADMIN.getValue(), "c",
                List.of(), null, null);
        OAuth2CodeDO db = codeMapper.selectByCode(code.getCode());
        codeMapper.updateById(new OAuth2CodeDO().setId(db.getId()).setExpiresTime(LocalDateTime.now().minusSeconds(1)));

        assertServiceException(() -> codeService.consumeAuthorizationCode(code.getCode()), OAUTH2_CODE_EXPIRE);
        // 过期的授权码同样作废
        assertServiceException(() -> codeService.consumeAuthorizationCode(code.getCode()), OAUTH2_CODE_NOT_EXISTS);
    }

    @Test
    void code不存在() {
        assertServiceException(() -> codeService.consumeAuthorizationCode("none"), OAUTH2_CODE_NOT_EXISTS);
    }
}

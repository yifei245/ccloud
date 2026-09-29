package com.ccloud.module.system.service.oauth2;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ApproveMapper;
import com.ccloud.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ccloud.module.system.service.oauth2.OAuth2TestData.CLIENT_ID;
import static com.ccloud.module.system.service.oauth2.OAuth2TestData.client;
import static org.assertj.core.api.Assertions.assertThat;

@Import({OAuth2ApproveServiceImpl.class, OAuth2ClientServiceImpl.class})
class OAuth2ApproveServiceImplTest extends BaseSystemDbUnitTest {

    private static final Long USER_ID = 1L;
    private static final Integer USER_TYPE = UserTypeEnum.ADMIN.getValue();

    @Resource
    private OAuth2ApproveService approveService;
    @Resource
    private OAuth2ApproveMapper approveMapper;
    @Resource
    private OAuth2ClientMapper clientMapper;

    @BeforeEach
    void seed() {
        clientMapper.insert(client());
    }

    @Test
    void checkForPreApproval_申请范围全部在自动授权范围内时通过并记录() {
        assertThat(approveService.checkForPreApproval(USER_ID, USER_TYPE, CLIENT_ID, List.of("user.read"))).isTrue();
        assertThat(approveService.getApproveList(USER_ID, USER_TYPE, CLIENT_ID))
                .extracting(OAuth2ApproveDO::getScope).containsExactly("user.read");
    }

    @Test
    void checkForPreApproval_包含未自动授权且未同意过的范围时不通过() {
        assertThat(approveService.checkForPreApproval(USER_ID, USER_TYPE, CLIENT_ID,
                List.of("user.read", "user.write"))).isFalse();
    }

    @Test
    void checkForPreApproval_用户此前已同意时通过_拒绝或过期的不算() {
        approveService.updateAfterApproval(USER_ID, USER_TYPE, CLIENT_ID, map("user.write", true));
        assertThat(approveService.checkForPreApproval(USER_ID, USER_TYPE, CLIENT_ID, List.of("user.write"))).isTrue();

        approveService.updateAfterApproval(USER_ID, USER_TYPE, CLIENT_ID, map("user.write", false));
        assertThat(approveService.checkForPreApproval(USER_ID, USER_TYPE, CLIENT_ID, List.of("user.write"))).isFalse();

        approveService.updateAfterApproval(USER_ID, USER_TYPE, CLIENT_ID, map("user.write", true));
        OAuth2ApproveDO approve = approveMapper.selectOne(USER_ID, USER_TYPE, CLIENT_ID, "user.write");
        approveMapper.updateById(new OAuth2ApproveDO().setId(approve.getId()).setExpiresTime(LocalDateTime.now().minusSeconds(1)));
        assertThat(approveService.checkForPreApproval(USER_ID, USER_TYPE, CLIENT_ID, List.of("user.write"))).isFalse();
    }

    @Test
    void updateAfterApproval_任一范围同意即返回true_同一范围重复提交不产生重复记录() {
        assertThat(approveService.updateAfterApproval(USER_ID, USER_TYPE, CLIENT_ID,
                map("user.read", false, "user.write", true))).isTrue();
        assertThat(approveService.updateAfterApproval(USER_ID, USER_TYPE, CLIENT_ID,
                map("user.read", false, "user.write", false))).isFalse();
        assertThat(approveMapper.selectCount()).isEqualTo(2);
    }

    @Test
    void updateAfterApproval_没有申请任何范围时视为通过() {
        assertThat(approveService.updateAfterApproval(USER_ID, USER_TYPE, CLIENT_ID, Map.of())).isTrue();
    }

    private static Map<String, Boolean> map(Object... kv) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], (Boolean) kv[i + 1]);
        }
        return map;
    }
}

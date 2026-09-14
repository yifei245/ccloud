package com.ccloud.module.system.enums;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.module.system.SqlFixture;
import com.ccloud.module.system.enums.logger.LoginLogTypeEnum;
import com.ccloud.module.system.enums.logger.LoginResultEnum;
import com.ccloud.module.system.enums.oauth2.OAuth2ClientConstants;
import com.ccloud.module.system.enums.oauth2.OAuth2GrantTypeEnum;
import com.ccloud.module.system.enums.permission.DataScopeEnum;
import com.ccloud.module.system.enums.permission.MenuTypeEnum;
import com.ccloud.module.system.enums.permission.RoleCodeEnum;
import com.ccloud.module.system.enums.permission.RoleTypeEnum;
import com.ccloud.module.system.enums.sms.SmsSceneEnum;
import com.ccloud.module.system.enums.sms.SmsSendStatusEnum;
import com.ccloud.module.system.enums.sms.SmsTemplateTypeEnum;
import com.ccloud.module.system.enums.social.SocialTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 枚举与字典数据的一致性。字典数据驱动前端下拉框与标签展示，
 * 枚举驱动后端业务判断——两边取值必须一致，否则界面能选出后端不认识的值。
 */
class EnumDictConsistencyTest {

    private static <E extends Enum<E>> Set<String> values(Class<E> type, Function<E, Object> getter) {
        return Arrays.stream(type.getEnumConstants()).map(getter).map(String::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("通用状态与用户类型的枚举应与字典一致")
    void commonEnums() {
        assertThat(values(CommonStatusEnum.class, CommonStatusEnum::getStatus))
                .isEqualTo(SqlFixture.dictValues(CommonStatusEnum.DICT_TYPE));
        assertThat(values(UserTypeEnum.class, UserTypeEnum::getValue))
                .isEqualTo(SqlFixture.dictValues(UserTypeEnum.DICT_TYPE));
    }

    @Test
    @DisplayName("权限相关枚举应与字典一致")
    void permissionEnums() {
        assertThat(values(DataScopeEnum.class, DataScopeEnum::getScope))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.DATA_SCOPE));
        assertThat(values(MenuTypeEnum.class, MenuTypeEnum::getType))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.MENU_TYPE));
        assertThat(values(RoleTypeEnum.class, RoleTypeEnum::getType))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.ROLE_TYPE));
    }

    @Test
    @DisplayName("日志相关枚举应与字典一致")
    void loggerEnums() {
        assertThat(values(LoginLogTypeEnum.class, LoginLogTypeEnum::getType))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.LOGIN_TYPE));
        assertThat(values(LoginResultEnum.class, LoginResultEnum::getResult))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.LOGIN_RESULT));
    }

    @Test
    @DisplayName("短信相关枚举应与字典一致")
    void smsEnums() {
        assertThat(values(SmsSceneEnum.class, SmsSceneEnum::getScene))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.SMS_SCENE));
        assertThat(values(SmsSendStatusEnum.class, SmsSendStatusEnum::getStatus))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.SMS_SEND_STATUS));
        assertThat(values(SmsTemplateTypeEnum.class, SmsTemplateTypeEnum::getType))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.SMS_TEMPLATE_TYPE));
    }

    @Test
    @DisplayName("社交类型与 OAuth2 授权类型枚举应与字典一致")
    void socialAndOAuth2Enums() {
        assertThat(values(SocialTypeEnum.class, SocialTypeEnum::getType))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.SOCIAL_TYPE));
        assertThat(values(OAuth2GrantTypeEnum.class, OAuth2GrantTypeEnum::getGrantType))
                .isEqualTo(SqlFixture.dictValues(DictTypeConstants.OAUTH2_GRANT_TYPE));
    }

    @Test
    @DisplayName("每个短信场景对应的模板编码都应在初始化数据中存在，否则发验证码时找不到模板")
    void smsSceneTemplatesShouldExist() {
        for (SmsSceneEnum scene : SmsSceneEnum.values()) {
            assertThat(SqlFixture.contains("07-demo.sql", "'" + scene.getTemplateCode() + "'"))
                    .as("短信场景 %s 的模板 %s", scene, scene.getTemplateCode()).isTrue();
        }
    }

    @Test
    @DisplayName("内置角色编码与客户端编号都应在初始化数据中存在")
    void builtinIdentifiersShouldExist() {
        assertThat(SqlFixture.contains("03-role.sql", "'" + RoleCodeEnum.SUPER_ADMIN.getCode() + "'")).isTrue();
        assertThat(SqlFixture.contains("03-role.sql", "'" + RoleCodeEnum.COMMON_ADMIN.getCode() + "'")).isTrue();
        for (String clientId : OAuth2ClientConstants.ALL_CLIENT_IDS) {
            assertThat(SqlFixture.contains("05-oauth2-client.sql", "'" + clientId + "'")).as(clientId).isTrue();
        }
    }
}

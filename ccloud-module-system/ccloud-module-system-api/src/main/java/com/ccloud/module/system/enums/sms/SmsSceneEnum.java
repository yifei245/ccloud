package com.ccloud.module.system.enums.sms;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 短信验证码发送场景。每个场景绑定一个模板编码，模板须在初始化数据中存在
 */
@Getter
@AllArgsConstructor
public enum SmsSceneEnum implements IntArrayValuable {

    MEMBER_LOGIN(1, "会员-手机号登录", "user-sms-login"),
    MEMBER_UPDATE_MOBILE(2, "会员-修改手机", "user-update-mobile"),
    MEMBER_UPDATE_PASSWORD(3, "会员-修改密码", "user-update-password"),
    MEMBER_RESET_PASSWORD(4, "会员-忘记密码", "user-reset-password"),
    ADMIN_MEMBER_LOGIN(21, "后台-手机号登录", "admin-sms-login"),
    ADMIN_MEMBER_RESET_PASSWORD(22, "后台-忘记密码", "admin-reset-password");

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(SmsSceneEnum::getScene).toArray();

    private final Integer scene;
    private final String name;

    /**
     * 对应的短信模板编码
     */
    private final String templateCode;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static SmsSceneEnum valueOf(Integer value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.scene, value)).findFirst().orElse(null);
    }
}

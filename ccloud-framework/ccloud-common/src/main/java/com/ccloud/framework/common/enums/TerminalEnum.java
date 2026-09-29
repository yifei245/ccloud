package com.ccloud.framework.common.enums;

import com.ccloud.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 终端类型。用于区分请求来源，落在注册记录与登录日志里，也影响部分业务策略。
 */
@Getter
@AllArgsConstructor
public enum TerminalEnum implements IntArrayValuable {

    UNKNOWN(0, "未知"),
    WECHAT_MINI_PROGRAM(10, "微信小程序"),
    WECHAT_WAP(11, "微信公众号"),
    H5(20, "H5 网页"),
    APP(31, "手机 App"),
    APP_IOS(32, "iOS App"),
    APP_ANDROID(33, "Android App"),
    WEB(40, "PC 网页");

    public static final String DICT_TYPE = "terminal";

    private static final int[] ARRAYS = Arrays.stream(values()).mapToInt(TerminalEnum::getTerminal).toArray();

    private final Integer terminal;
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }
}

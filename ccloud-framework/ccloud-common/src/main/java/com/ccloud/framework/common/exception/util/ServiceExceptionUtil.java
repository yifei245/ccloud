package com.ccloud.framework.common.exception.util;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务异常构建工具。
 *
 * <p>维护一张可覆盖的错误码文案表：i18n starter 会把数据库里配置的文案灌进来，
 * 从而在不改代码的前提下调整提示语，也是错误提示国际化的落点。
 */
@Slf4j
public class ServiceExceptionUtil {

    /**
     * 错误码 -> 文案。为空时回退到 {@link ErrorCode#getMsg()}
     */
    private static final Map<Integer, String> MESSAGES = new ConcurrentHashMap<>();

    public static void putMessage(Integer code, String message) {
        MESSAGES.put(code, message);
    }

    public static void putAll(Map<Integer, String> messages) {
        if (messages != null) {
            MESSAGES.putAll(messages);
        }
    }

    public static void clearMessages() {
        MESSAGES.clear();
    }

    /**
     * 基于 ErrorCode 构建业务异常
     *
     * @param errorCode 错误码
     * @param params    文案中 {} 占位符的填充值
     */
    public static ServiceException exception(ErrorCode errorCode, Object... params) {
        String template = MESSAGES.getOrDefault(errorCode.getCode(), errorCode.getMsg());
        return new ServiceException(errorCode.getCode(), doFormat(errorCode.getCode(), template, params));
    }

    public static ServiceException exception(Integer code, String messagePattern, Object... params) {
        return new ServiceException(code, doFormat(code, messagePattern, params));
    }

    /**
     * 参数不合法异常，固定使用 400
     */
    public static ServiceException invalidParamException(String messagePattern, Object... params) {
        return exception(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), messagePattern, params);
    }

    /**
     * 格式化失败不应导致业务中断，因此这里吞掉异常并退化为原始模板
     */
    private static String doFormat(Integer code, String messagePattern, Object... params) {
        if (messagePattern == null) {
            return null;
        }
        try {
            return StrUtil.format(messagePattern, params);
        } catch (Exception e) {
            log.warn("[doFormat] 错误码 {} 的文案格式化失败，模板 [{}]", code, messagePattern, e);
            return messagePattern;
        }
    }
}

package com.ccloud.framework.common.util.validation;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 校验工具
 */
public class ValidationUtils {

    private static final Pattern PATTERN_URL = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].\\S*$");
    private static final Pattern PATTERN_XML_NCNAME = Pattern.compile("[a-zA-Z_][\\-_.0-9_a-zA-Z$]*");
    private static final Pattern PATTERN_MOBILE = Pattern.compile("^(?:\\+?86)?1(?:3\\d{3}|5[^4\\D]\\d{2}"
            + "|8\\d{3}|7(?:[0-35-9]\\d{2}|4(?:0\\d|1[0-2]|9\\d))|9[0-35-9]\\d{2}|6[2567]\\d{2}"
            + "|4(?:(?:10|4[01])\\d{3}|[68]\\d{4}|[579]\\d{2}))\\d{6}$");
    private static final Pattern PATTERN_TELEPHONE = Pattern.compile("^(?:(?:\\d{3}-)?\\d{8}|^(?:\\d{4}-)?\\d{7,8})(?:-\\d+)?$");

    public static boolean isMobile(String mobile) {
        return StrUtil.isNotBlank(mobile) && PATTERN_MOBILE.matcher(mobile).matches();
    }

    public static boolean isTelephone(String telephone) {
        return StrUtil.isNotBlank(telephone) && PATTERN_TELEPHONE.matcher(telephone).matches();
    }

    public static boolean isURL(String url) {
        return StrUtil.isNotBlank(url) && PATTERN_URL.matcher(url).matches();
    }

    public static boolean isXmlNCName(String str) {
        return StrUtil.isNotBlank(str) && PATTERN_XML_NCNAME.matcher(str).matches();
    }

    /**
     * 手工触发校验，用于 Service 层对非 Controller 入参的对象做校验
     */
    public static void validate(Validator validator, Object object, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}

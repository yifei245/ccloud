package com.ccloud.framework.apilog.core.annotation;

import com.ccloud.framework.apilog.core.enums.OperateTypeEnum;

import java.lang.annotation.*;

/**
 * 访问日志开关与元信息。
 *
 * <p>不加该注解时按全局配置决定是否记录；加了则以注解为准，
 * 并把模块名、操作名带进日志，便于运营侧检索。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiAccessLog {

    boolean enable() default true;

    /**
     * 是否记录请求参数。上传文件、超大报文等场景可关闭
     */
    boolean requestEnable() default true;

    /**
     * 是否记录响应体。返回大列表时建议关闭
     */
    boolean responseEnable() default false;

    /**
     * 敏感参数名，记录前会被替换为 ******，与内置敏感词（密码、令牌等）叠加生效
     */
    String[] sanitizeKeys() default {};

    String operateModule() default "";

    String operateName() default "";

    OperateTypeEnum operateType() default OperateTypeEnum.OTHER;
}

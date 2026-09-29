package com.ccloud.framework.operatelog.core.annotation;

import com.ccloud.framework.apilog.core.enums.OperateTypeEnum;

import java.lang.annotation.*;

/**
 * 操作日志：记录"谁在什么时候对哪条数据做了什么"，供审计追溯。
 *
 * <p>标注在 Controller 方法上，而不是 Service：Service 通常在事务内，
 * 业务失败回滚时失败记录也会随之回滚，恰好丢掉最需要追查的那部分。
 *
 * <p>参数与返回值以 JSON 记录，password、secret、token 等敏感字段会被脱敏。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {

    /**
     * 操作模块，如"用户管理"
     */
    String type();

    /**
     * 操作名，如"修改用户"
     */
    String subType();

    /**
     * 业务数据编号的 SpEL 表达式，按参数名引用，如 {@code #id}、{@code #reqVO.id}；
     * {@code #_ret} 引用返回值（仅成功时可用，如创建接口返回的新编号）。为空时记为 0
     */
    String bizId() default "";

    OperateTypeEnum operateType() default OperateTypeEnum.OTHER;

    /**
     * 是否记录请求参数。导入文件等大参数接口应关闭
     */
    boolean logArgs() default true;

    /**
     * 是否记录返回值。导出、分页等大结果接口应关闭
     */
    boolean logResult() default true;
}

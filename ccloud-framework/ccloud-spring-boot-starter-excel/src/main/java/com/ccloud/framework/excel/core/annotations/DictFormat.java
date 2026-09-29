package com.ccloud.framework.excel.core.annotations;

import java.lang.annotation.*;

/**
 * 声明字段对应的字典类型。
 *
 * <p>配合 {@code @ExcelProperty(converter = DictConvert.class)} 使用，
 * 导出时把库里的状态码翻译成"开启/关闭"，导入时再还原回码值。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DictFormat {

    /**
     * 字典类型，如 common_status
     */
    String value();
}

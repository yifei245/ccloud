package com.ccloud.framework.excel.core.convert;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.service.DictFrameworkService;
import lombok.extern.slf4j.Slf4j;

/**
 * 字典值与标签的双向转换器。
 *
 * <p>翻译失败时返回原值而非抛异常：导出通常是几千行数据，
 * 一条脏数据不该让整次导出失败，让用户在文件里看到原始码值反而更好排查。
 */
@Slf4j
public class DictConvert implements Converter<Object> {

    private static DictFrameworkService dictFrameworkService;

    public static void setDictFrameworkService(DictFrameworkService service) {
        dictFrameworkService = service;
    }

    public static String translateToLabel(String dictType, String value) {
        if (dictFrameworkService == null || StrUtil.isBlank(value)) {
            return value;
        }
        String label = dictFrameworkService.getDictDataLabel(dictType, value);
        return label != null ? label : value;
    }

    public static String translateToValue(String dictType, String label) {
        if (dictFrameworkService == null || StrUtil.isBlank(label)) {
            return label;
        }
        String value = dictFrameworkService.getDictDataValue(dictType, label);
        return value != null ? value : label;
    }

    @Override
    public Class<?> supportJavaTypeKey() {
        return Object.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                    GlobalConfiguration globalConfiguration) {
        String dictType = resolveDictType(contentProperty);
        String value = translateToValue(dictType, cellData.getStringValue());
        return convertType(value, contentProperty);
    }

    @Override
    public WriteCellData<String> convertToExcelData(Object object, ExcelContentProperty contentProperty,
                                                    GlobalConfiguration globalConfiguration) {
        if (object == null) {
            return new WriteCellData<>("");
        }
        String dictType = resolveDictType(contentProperty);
        return new WriteCellData<>(translateToLabel(dictType, String.valueOf(object)));
    }

    private static String resolveDictType(ExcelContentProperty contentProperty) {
        DictFormat dictFormat = contentProperty.getField().getAnnotation(DictFormat.class);
        if (dictFormat == null) {
            throw new IllegalStateException("字段 " + contentProperty.getField().getName()
                    + " 使用了 DictConvert 但缺少 @DictFormat 注解");
        }
        return dictFormat.value();
    }

    /**
     * 字典值在库里可能是 Integer 或 String，按字段实际类型还原
     */
    private static Object convertType(String value, ExcelContentProperty contentProperty) {
        if (value == null) {
            return null;
        }
        Class<?> fieldType = contentProperty.getField().getType();
        if (fieldType == Integer.class || fieldType == int.class) {
            return Integer.valueOf(value);
        }
        if (fieldType == Long.class || fieldType == long.class) {
            return Long.valueOf(value);
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return Boolean.valueOf(value);
        }
        return value;
    }
}

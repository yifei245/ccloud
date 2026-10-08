package com.ccloud.module.infra.service.codegen.inner;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.enums.codegen.CodegenColumnListConditionEnum;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板使用的字段视图：把"怎么写成代码"的判断集中在这里，模板里只做拼接
 */
@Getter
public class CodegenColumnView {

    private static final Pattern CHAR_LENGTH = Pattern.compile("^(?:var)?char\\((\\d+)\\)$");

    private final String columnName;
    private final String javaType;
    private final String javaField;
    private final String javaFieldUpper;
    /** 用于 Java 字符串字面量，已转义 */
    private final String comment;
    /** 用于 Javadoc，已去掉注释结束符 */
    private final String javadoc;
    /** 用于校验提示，取注释第一个分隔符前的部分，已转义 */
    private final String label;
    private final boolean primaryKey;
    private final boolean nullable;
    private final boolean string;
    private final boolean bigDecimal;
    private final boolean between;
    /** 时间类字段的格式，其他类型为 null */
    private final String dateTimePattern;
    private final String queryMethod;
    /** 为空串时为 null，模板用 #if 判断 */
    private final String dictType;
    private final String example;
    /** 字符类型的最大长度，由 dataType（如 varchar(100)）解析，其他为 null */
    private final Integer maxLength;
    /** 生成测试中的样例值（Java 表达式） */
    private final String sampleValue;

    public CodegenColumnView(CodegenColumnDO column, boolean treeParent) {
        this.columnName = column.getColumnName();
        this.javaType = column.getJavaType();
        this.javaField = column.getJavaField();
        this.javaFieldUpper = StrUtil.upperFirst(column.getJavaField());
        this.comment = escapeJava(column.getColumnComment());
        this.javadoc = column.getColumnComment().replace("*/", "* /");
        this.label = escapeJava(column.getColumnComment().trim().split("[，,（(：:；;。]", 2)[0].trim());
        this.primaryKey = Boolean.TRUE.equals(column.getPrimaryKey());
        this.nullable = Boolean.TRUE.equals(column.getNullable());
        this.string = "String".equals(javaType);
        this.bigDecimal = "BigDecimal".equals(javaType);
        this.between = CodegenColumnListConditionEnum.BETWEEN.getCondition().equals(column.getListOperationCondition());
        this.dateTimePattern = switch (javaType) {
            case "LocalDateTime" -> "yyyy-MM-dd HH:mm:ss";
            case "LocalDate" -> "yyyy-MM-dd";
            case "LocalTime" -> "HH:mm:ss";
            default -> null;
        };
        this.queryMethod = CodegenColumnListConditionEnum.valueOfCondition(column.getListOperationCondition()).getQueryMethod();
        this.dictType = StrUtil.emptyToNull(column.getDictType());
        this.example = StrUtil.isEmpty(column.getExample()) ? null : escapeJava(column.getExample());
        this.maxLength = parseLength(column.getDataType());
        this.sampleValue = treeParent ? "0L" : sampleValue(javaType, column.getDataType(), maxLength);
    }

    static String escapeJava(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    private static Integer parseLength(String dataType) {
        Matcher m = CHAR_LENGTH.matcher(StrUtil.nullToEmpty(dataType));
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    private static String sampleValue(String javaType, String dataType, Integer maxLength) {
        return switch (javaType) {
            case "String" -> dataType.startsWith("json") ? "\"{}\"" : (maxLength != null && maxLength < 2 ? "\"a\"" : "\"样例\"");
            case "Long" -> "1L";
            case "Integer" -> "1";
            case "Boolean" -> "true";
            case "BigDecimal" -> "new BigDecimal(\"1\")";
            case "Float" -> "1.0F";
            case "Double" -> "1.0D";
            case "LocalDateTime" -> "LocalDateTime.now().withNano(0)";
            case "LocalDate" -> "LocalDate.now()";
            case "LocalTime" -> "LocalTime.of(8, 0)";
            default -> throw new IllegalArgumentException("不支持的类型 " + javaType);
        };
    }
}

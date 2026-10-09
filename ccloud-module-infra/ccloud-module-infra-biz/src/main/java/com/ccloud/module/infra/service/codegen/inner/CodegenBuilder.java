package com.ccloud.module.infra.service.codegen.inner;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.enums.codegen.CodegenColumnHtmlTypeEnum;
import com.ccloud.module.infra.enums.codegen.CodegenColumnListConditionEnum;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

/**
 * 由数据库表结构推断代码生成的默认定义。只做推断，不访问数据库，便于单测
 */
@Component
public class CodegenBuilder {

    public static final Set<String> BASE_COLUMNS =
            Set.of("creator", "create_time", "updater", "update_time", "deleted", "tenant_id");

    static final String DEFAULT_AUTHOR = "ccloud";
    /**
     * 本项目不做前端，前端类型固定为 0
     */
    static final int FRONT_TYPE_NONE = 0;

    private static final Map<String, String> JAVA_TYPES = Map.ofEntries(
            Map.entry("bigint", "Long"),
            Map.entry("int", "Integer"), Map.entry("integer", "Integer"), Map.entry("mediumint", "Integer"),
            Map.entry("smallint", "Integer"), Map.entry("tinyint", "Integer"),
            Map.entry("bit", "Boolean"),
            Map.entry("decimal", "BigDecimal"), Map.entry("numeric", "BigDecimal"),
            Map.entry("float", "Float"), Map.entry("double", "Double"),
            Map.entry("datetime", "LocalDateTime"), Map.entry("timestamp", "LocalDateTime"),
            Map.entry("date", "LocalDate"), Map.entry("time", "LocalTime"),
            Map.entry("char", "String"), Map.entry("varchar", "String"), Map.entry("tinytext", "String"),
            Map.entry("text", "String"), Map.entry("mediumtext", "String"), Map.entry("longtext", "String"),
            Map.entry("json", "String"));

    public CodegenTableDO buildTable(Long dataSourceConfigId, TableInfo table) {
        String[] parts = table.name().split("_", 2);
        if (parts.length < 2 || StrUtil.isBlank(parts[0]) || StrUtil.isBlank(parts[1])) {
            throw exception(CODEGEN_TABLE_NAME_INVALID);
        }
        if (StrUtil.isBlank(table.comment())) {
            throw exception(CODEGEN_TABLE_INFO_TABLE_COMMENT_IS_NULL);
        }
        String rest = parts[1].toLowerCase(Locale.ROOT);
        return new CodegenTableDO().setDataSourceConfigId(dataSourceConfigId)
                .setScene(CodegenSceneEnum.ADMIN.getScene()).setTemplateType(CodegenTemplateTypeEnum.ONE.getType())
                .setFrontType(FRONT_TYPE_NONE).setAuthor(DEFAULT_AUTHOR)
                .setTableName(table.name()).setTableComment(table.comment())
                .setModuleName(parts[0].toLowerCase(Locale.ROOT))
                .setBusinessName(StrUtil.subBefore(rest, "_", false))
                .setClassName(StrUtil.upperFirst(StrUtil.toCamelCase(rest)))
                .setClassComment(toClassComment(table.comment()));
    }

    /**
     * "积分记录表，按用户分表" → "积分记录"：取第一个分隔符前的部分，再去掉结尾的"表"
     */
    static String toClassComment(String comment) {
        String head = comment.trim().split("[，,（(：:；;。]", 2)[0].trim();
        return StrUtil.blankToDefault(StrUtil.removeSuffix(head, "表"), head);
    }

    public List<CodegenColumnDO> buildColumns(Long tableId, List<ColumnInfo> columns) {
        List<ColumnInfo> pks = columns.stream().filter(ColumnInfo::primaryKey).toList();
        if (pks.size() != 1 || !pks.get(0).autoIncrement() || !"Long".equals(javaType(pks.get(0)))) {
            throw exception(CODEGEN_PRIMARY_KEY_REQUIRED);
        }
        List<CodegenColumnDO> result = new ArrayList<>(columns.size());
        for (ColumnInfo column : columns) {
            if (StrUtil.isBlank(column.comment())) {
                throw exception(CODEGEN_TABLE_INFO_COLUMN_COMMENT_IS_NULL, column.name());
            }
            String javaType = javaType(column);
            if (javaType == null) {
                throw exception(CODEGEN_COLUMN_TYPE_UNSUPPORTED, column.name(), column.columnType());
            }
            boolean base = BASE_COLUMNS.contains(column.name());
            boolean editable = !column.primaryKey() && !base;
            String condition = defaultCondition(column, javaType);
            result.add(new CodegenColumnDO().setTableId(tableId).setColumnName(column.name())
                    .setDataType(column.columnType()).setColumnComment(column.comment())
                    .setNullable(column.nullable()).setPrimaryKey(column.primaryKey())
                    .setOrdinalPosition(column.ordinalPosition()).setJavaType(javaType)
                    .setJavaField(StrUtil.toCamelCase(column.name())).setDictType("")
                    .setCreateOperation(editable).setUpdateOperation(editable)
                    .setListOperation(condition != null)
                    .setListOperationCondition(condition != null ? condition : CodegenColumnListConditionEnum.EQ.getCondition())
                    .setListOperationResult(!base || "create_time".equals(column.name()))
                    .setHtmlType(defaultHtmlType(column, javaType)));
        }
        return result;
    }

    /**
     * @return Java 类型；不支持返回 null
     */
    public static String javaType(ColumnInfo column) {
        return JAVA_TYPES.get(column.dataType());
    }

    /**
     * @return 默认作为查询条件时的比较方式；不作为查询条件返回 null
     */
    private static String defaultCondition(ColumnInfo column, String javaType) {
        String name = column.name();
        if ("create_time".equals(name)) {
            return CodegenColumnListConditionEnum.BETWEEN.getCondition();
        }
        if (column.primaryKey() || BASE_COLUMNS.contains(name)) {
            return null;
        }
        if ("String".equals(javaType) && (name.equals("name") || name.endsWith("_name") || name.equals("title"))) {
            return CodegenColumnListConditionEnum.LIKE.getCondition();
        }
        if (name.equals("status") || name.endsWith("_status") || name.equals("type") || name.endsWith("_type")
                || name.equals("sex")) {
            return CodegenColumnListConditionEnum.EQ.getCondition();
        }
        return null;
    }

    private static String defaultHtmlType(ColumnInfo column, String javaType) {
        return switch (javaType) {
            case "LocalDateTime", "LocalDate", "LocalTime" -> CodegenColumnHtmlTypeEnum.DATETIME.getType();
            case "Boolean" -> CodegenColumnHtmlTypeEnum.RADIO.getType();
            case "String" -> column.charMaxLength() == null || column.charMaxLength() > 500
                    ? CodegenColumnHtmlTypeEnum.TEXTAREA.getType() : CodegenColumnHtmlTypeEnum.INPUT.getType();
            default -> column.name().matches(".*(status|type|sex)$")
                    ? CodegenColumnHtmlTypeEnum.SELECT.getType() : CodegenColumnHtmlTypeEnum.INPUT.getType();
        };
    }
}

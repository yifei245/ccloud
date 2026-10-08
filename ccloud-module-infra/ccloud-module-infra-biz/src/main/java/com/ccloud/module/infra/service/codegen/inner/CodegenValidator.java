package com.ccloud.module.infra.service.codegen.inner;

import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.enums.codegen.CodegenColumnListConditionEnum;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;

import javax.lang.model.SourceVersion;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

/**
 * 代码生成配置的校验：模块名、业务名、类名、属性名会写进文件路径、包名与源码，
 * 不合法时生成的代码无法编译，甚至能逃出压缩包的根目录或注入代码。保存配置与生成代码时都要校验
 */
public final class CodegenValidator {

    public static final String PACKAGE_REGEX = "^[a-z][a-z0-9]*$";
    public static final String CLASS_NAME_REGEX = "^[A-Z][A-Za-z0-9]*$";
    public static final String JAVA_FIELD_REGEX = "^[a-z][A-Za-z0-9]*$";
    /**
     * 模板支持的 Java 类型，须与 CodegenColumnView 的样例值一致
     */
    public static final String JAVA_TYPE_REGEX =
            "^(String|Long|Integer|Boolean|BigDecimal|Float|Double|LocalDateTime|LocalDate|LocalTime)$";

    private static final Pattern PACKAGE = Pattern.compile(PACKAGE_REGEX);
    private static final Pattern CLASS_NAME = Pattern.compile(CLASS_NAME_REGEX);
    private static final Pattern JAVA_FIELD = Pattern.compile(JAVA_FIELD_REGEX);
    private static final Pattern JAVA_TYPE = Pattern.compile(JAVA_TYPE_REGEX);
    private static final Pattern TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    private CodegenValidator() {
    }

    public static void validate(CodegenTableDO table, List<CodegenColumnDO> columns) {
        if (CodegenSceneEnum.valueOf(table.getScene()) == null) {
            throw invalid("生成场景 " + table.getScene());
        }
        CodegenTemplateTypeEnum templateType = CodegenTemplateTypeEnum.valueOf(table.getTemplateType());
        if (templateType == null) {
            throw invalid("模板类型 " + table.getTemplateType());
        }
        validateName("表名", table.getTableName(), TABLE_NAME);
        validateName("模块名", table.getModuleName(), PACKAGE);
        validateName("业务名", table.getBusinessName(), PACKAGE);
        validateName("类名", table.getClassName(), CLASS_NAME);

        List<CodegenColumnDO> pks = columns.stream().filter(c -> Boolean.TRUE.equals(c.getPrimaryKey())).toList();
        if (pks.size() != 1 || !"Long".equals(pks.get(0).getJavaType())) {
            throw exception(CODEGEN_PRIMARY_KEY_REQUIRED);
        }
        Set<String> fields = new HashSet<>();
        for (CodegenColumnDO column : columns) {
            validateName("字段 " + column.getColumnName() + " 的 Java 属性名", column.getJavaField(), JAVA_FIELD);
            if (!fields.add(column.getJavaField())) {
                throw invalid("Java 属性名 " + column.getJavaField() + " 重复");
            }
            validateName("字段 " + column.getColumnName() + " 的 Java 类型", column.getJavaType(), JAVA_TYPE);
            CodegenColumnListConditionEnum condition =
                    CodegenColumnListConditionEnum.valueOfCondition(column.getListOperationCondition());
            if (condition == null) {
                throw invalid("字段 " + column.getColumnName() + " 的查询条件 " + column.getListOperationCondition());
            }
            // likeIfPresent 的参数是 String，其他类型生成的代码无法编译
            if (condition == CodegenColumnListConditionEnum.LIKE && Boolean.TRUE.equals(column.getListOperation())
                    && !"String".equals(column.getJavaType())) {
                throw invalid("字段 " + column.getColumnName() + " 不是字符串，不能用 LIKE 查询");
            }
        }
        if (templateType == CodegenTemplateTypeEnum.TREE) {
            validateTreeColumns(table, columns);
        }
    }

    /**
     * 树表的父编号字段、名称字段须在本表字段中，父编号须为 Long（生成的父节点校验按 Long 编写），
     * 且用于新增与修改（生成的代码从新增/修改 VO 中读取父编号）。
     * 表定义可能在字段同步后残留已删除字段的编号，这里报业务错误而不是空指针
     */
    private static void validateTreeColumns(CodegenTableDO table, List<CodegenColumnDO> columns) {
        CodegenColumnDO parent = findColumn(columns, table.getTreeParentColumnId());
        CodegenColumnDO name = findColumn(columns, table.getTreeNameColumnId());
        if (parent == null || name == null || !"Long".equals(parent.getJavaType())) {
            throw exception(CODEGEN_TREE_COLUMN_REQUIRED);
        }
        if (!Boolean.TRUE.equals(parent.getCreateOperation()) || !Boolean.TRUE.equals(parent.getUpdateOperation())) {
            throw invalid("树表的父编号字段须用于新增与修改");
        }
    }

    private static CodegenColumnDO findColumn(List<CodegenColumnDO> columns, Long id) {
        return id == null ? null : columns.stream().filter(c -> Objects.equals(id, c.getId())).findFirst().orElse(null);
    }

    private static void validateName(String label, String value, Pattern pattern) {
        // 关键字（如 int、class）符合格式，但不能作为包名或属性名
        if (value == null || !pattern.matcher(value).matches() || SourceVersion.isKeyword(value)) {
            throw invalid(label + " " + value);
        }
    }

    private static RuntimeException invalid(String detail) {
        return exception(CODEGEN_INVALID_IDENTIFIER, detail);
    }
}

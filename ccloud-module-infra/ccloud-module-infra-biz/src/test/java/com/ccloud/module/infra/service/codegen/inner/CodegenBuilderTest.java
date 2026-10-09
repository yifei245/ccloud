package com.ccloud.module.infra.service.codegen.inner;

import com.ccloud.framework.common.exception.ErrorCode;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodegenBuilderTest {

    private final CodegenBuilder builder = new CodegenBuilder();

    private static ColumnInfo col(String name, String columnType, String dataType, boolean nullable, boolean pk,
                                  boolean autoIncrement, int position, Long charMaxLength) {
        return new ColumnInfo(name, columnType, dataType, name + "注释", nullable, null, pk, autoIncrement, position, charMaxLength);
    }

    private static List<ColumnInfo> studentColumns() {
        return List.of(
                col("id", "bigint", "bigint", false, true, true, 1, null),
                col("name", "varchar(100)", "varchar", false, false, false, 2, 100L),
                col("sex", "tinyint", "tinyint", false, false, false, 3, null),
                col("birthday", "datetime", "datetime", true, false, false, 4, null),
                col("status", "tinyint", "tinyint", false, false, false, 5, null),
                col("price", "decimal(10,2)", "decimal", true, false, false, 6, null),
                col("enabled", "bit(1)", "bit", false, false, false, 7, null),
                col("creator", "varchar(64)", "varchar", true, false, false, 8, 64L),
                col("create_time", "datetime", "datetime", false, false, false, 9, null),
                col("deleted", "bigint", "bigint", false, false, false, 10, null));
    }

    private static void assertServiceException(ThrowableAssert.ThrowingCallable callable, ErrorCode errorCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(errorCode.getCode());
    }

    @Test
    void buildTable_由表名推断模块业务与类名_类描述去掉表字与补充说明() {
        CodegenTableDO table = builder.buildTable(0L, new TableInfo("member_point_record", "积分记录表，按用户分表", List.of(), List.of()));
        assertThat(table.getModuleName()).isEqualTo("member");
        assertThat(table.getBusinessName()).isEqualTo("point");
        assertThat(table.getClassName()).isEqualTo("PointRecord");
        assertThat(table.getClassComment()).isEqualTo("积分记录");
        assertThat(table.getScene()).isEqualTo(CodegenSceneEnum.ADMIN.getScene());
        assertThat(table.getTemplateType()).isEqualTo(CodegenTemplateTypeEnum.ONE.getType());
        assertThat(table.getDataSourceConfigId()).isZero();
    }

    @Test
    void buildTable_表名不是模块_业务格式或无注释时报错() {
        assertServiceException(() -> builder.buildTable(0L, new TableInfo("users", "用户", List.of(), List.of())),
                CODEGEN_TABLE_NAME_INVALID);
        assertServiceException(() -> builder.buildTable(0L, new TableInfo("demo_user", " ", List.of(), List.of())),
                CODEGEN_TABLE_INFO_TABLE_COMMENT_IS_NULL);
    }

    @Test
    void buildColumns_类型映射与默认用途() {
        Map<String, CodegenColumnDO> columns = builder.buildColumns(9L, studentColumns()).stream()
                .collect(Collectors.toMap(CodegenColumnDO::getColumnName, c -> c));
        assertThat(columns.get("id").getJavaType()).isEqualTo("Long");
        assertThat(columns.get("sex").getJavaType()).isEqualTo("Integer");
        assertThat(columns.get("birthday").getJavaType()).isEqualTo("LocalDateTime");
        assertThat(columns.get("price").getJavaType()).isEqualTo("BigDecimal");
        assertThat(columns.get("enabled").getJavaType()).isEqualTo("Boolean");
        assertThat(columns.get("create_time").getJavaField()).isEqualTo("createTime");
        assertThat(columns.get("name").getDataType()).isEqualTo("varchar(100)");
        // 主键：不参与新增，参与结果
        assertThat(columns.get("id").getCreateOperation()).isFalse();
        assertThat(columns.get("id").getListOperationResult()).isTrue();
        // 普通字段：参与增改与结果
        assertThat(columns.get("sex").getCreateOperation()).isTrue();
        assertThat(columns.get("sex").getUpdateOperation()).isTrue();
        // 查询条件：名称模糊、状态与性别等值、创建时间区间，其余不作为条件
        assertThat(columns.get("name").getListOperationCondition()).isEqualTo("LIKE");
        assertThat(columns.get("status").getListOperationCondition()).isEqualTo("=");
        assertThat(columns.get("sex").getListOperation()).isTrue();
        assertThat(columns.get("create_time").getListOperation()).isTrue();
        assertThat(columns.get("create_time").getListOperationCondition()).isEqualTo("BETWEEN");
        assertThat(columns.get("birthday").getListOperation()).isFalse();
        // 公共字段：不参与增改；除创建时间外不出现在结果里
        assertThat(columns.get("creator").getCreateOperation()).isFalse();
        assertThat(columns.get("creator").getListOperationResult()).isFalse();
        assertThat(columns.get("deleted").getListOperationResult()).isFalse();
        assertThat(columns.get("create_time").getListOperationResult()).isTrue();
        assertThat(columns.get("sex").getTableId()).isEqualTo(9L);
    }

    @Test
    void buildColumns_主键必须唯一且为自增bigint_字段须有注释_类型须受支持() {
        List<ColumnInfo> noPk = studentColumns().stream().filter(c -> !c.primaryKey()).toList();
        assertServiceException(() -> builder.buildColumns(1L, noPk), CODEGEN_PRIMARY_KEY_REQUIRED);
        List<ColumnInfo> intPk = new ArrayList<>(noPk);
        intPk.add(col("id", "int", "int", false, true, true, 1, null));
        assertServiceException(() -> builder.buildColumns(1L, intPk), CODEGEN_PRIMARY_KEY_REQUIRED);

        List<ColumnInfo> noComment = new ArrayList<>(studentColumns());
        noComment.add(new ColumnInfo("remark", "varchar(10)", "varchar", "", true, null, false, false, 11, 10L));
        assertServiceException(() -> builder.buildColumns(1L, noComment), CODEGEN_TABLE_INFO_COLUMN_COMMENT_IS_NULL);

        List<ColumnInfo> geometry = new ArrayList<>(studentColumns());
        geometry.add(col("location", "point", "point", true, false, false, 11, null));
        assertServiceException(() -> builder.buildColumns(1L, geometry), CODEGEN_COLUMN_TYPE_UNSUPPORTED);
    }
}

package com.ccloud.module.infra.service.codegen.inner;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.ccloud.module.infra.service.db.dto.ColumnInfo;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodegenEngineTest {

    private final CodegenEngine engine = new CodegenEngine();
    private final CodegenBuilder builder = new CodegenBuilder();

    private static final List<ColumnInfo> STUDENT = List.of(
            new ColumnInfo("id", "bigint", "bigint", "编号", false, null, true, true, 1, null),
            new ColumnInfo("name", "varchar(100)", "varchar", "姓名", false, null, false, false, 2, 100L),
            new ColumnInfo("sex", "tinyint", "tinyint", "性别：1 男 2 女", false, null, false, false, 3, null),
            new ColumnInfo("birthday", "datetime", "datetime", "出生日期", true, null, false, false, 4, null),
            new ColumnInfo("score", "decimal(5,1)", "decimal", "成绩", true, null, false, false, 5, null),
            new ColumnInfo("tenant_id", "bigint", "bigint", "租户编号", false, "0", false, false, 6, null),
            new ColumnInfo("create_time", "datetime", "datetime", "创建时间", false, null, false, false, 7, null),
            new ColumnInfo("deleted", "bigint", "bigint", "是否删除", false, "0", false, false, 8, null));

    private static final List<ColumnInfo> CATEGORY = List.of(
            new ColumnInfo("id", "bigint", "bigint", "编号", false, null, true, true, 1, null),
            new ColumnInfo("name", "varchar(100)", "varchar", "名称", false, null, false, false, 2, 100L),
            new ColumnInfo("parent_id", "bigint", "bigint", "父编号，0 表示根节点", false, "0", false, false, 3, null),
            new ColumnInfo("create_time", "datetime", "datetime", "创建时间", false, null, false, false, 4, null));

    private Map<String, String> generate(String tableName, String comment, List<ColumnInfo> columnInfos,
                                         CodegenSceneEnum scene, boolean tree) {
        CodegenTableDO table = builder.buildTable(0L, new TableInfo(tableName, comment, List.of(), List.of()))
                .setId(5L).setScene(scene.getScene());
        List<CodegenColumnDO> columns = builder.buildColumns(5L, columnInfos);
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setId(100L + i);
        }
        if (tree) {
            table.setTemplateType(CodegenTemplateTypeEnum.TREE.getType())
                    .setTreeParentColumnId(findId(columns, "parent_id")).setTreeNameColumnId(findId(columns, "name"));
        }
        return engine.execute(table, columns);
    }

    @Test
    void 单表管理后台_生成全部文件_路径与类名符合约定() {
        Map<String, String> files = generate("infra_demo_student", "演示学生表", STUDENT, CodegenSceneEnum.ADMIN, false);
        String biz = "ccloud-module-infra/ccloud-module-infra-biz/src/main/java/com/ccloud/module/infra/";
        assertThat(files).containsKeys(
                biz + "dal/dataobject/demo/DemoStudentDO.java",
                biz + "dal/mysql/demo/DemoStudentMapper.java",
                biz + "service/demo/DemoStudentService.java",
                biz + "service/demo/DemoStudentServiceImpl.java",
                biz + "controller/admin/demo/DemoStudentController.java",
                biz + "controller/admin/demo/vo/DemoStudentPageReqVO.java",
                biz + "controller/admin/demo/vo/DemoStudentRespVO.java",
                biz + "controller/admin/demo/vo/DemoStudentSaveReqVO.java",
                "ccloud-module-infra/ccloud-module-infra-biz/src/test/java/com/ccloud/module/infra/service/demo/DemoStudentServiceImplTest.java",
                "ccloud-module-infra/ccloud-module-infra-api/src/main/java/com/ccloud/module/infra/enums/ErrorCodeConstants_DemoStudent.java.txt",
                "sql/codegen/infra_demo_student.sql");
        String dataObject = files.get(biz + "dal/dataobject/demo/DemoStudentDO.java");
        assertThat(dataObject).contains("public class DemoStudentDO extends TenantBaseDO", "@TableName(\"infra_demo_student\")",
                "private LocalDateTime birthday;", "private BigDecimal score;", "import java.math.BigDecimal;")
                .doesNotContain("tenantId", "createTime", "private Long deleted");
        String controller = files.get(biz + "controller/admin/demo/DemoStudentController.java");
        assertThat(controller).contains("@RequestMapping(\"/admin-api/infra/demo-student\")",
                "@PreAuthorize(\"@ss.hasPermission('infra:demo-student:create')\")", "/export-excel");
        assertThat(files.get(biz + "dal/mysql/demo/DemoStudentMapper.java"))
                .contains(".likeIfPresent(DemoStudentDO::getName, reqVO.getName())",
                        ".eqIfPresent(DemoStudentDO::getSex, reqVO.getSex())",
                        ".betweenIfPresent(DemoStudentDO::getCreateTime, reqVO.getCreateTime())");
        assertThat(files.get(biz + "controller/admin/demo/vo/DemoStudentSaveReqVO.java"))
                .contains("@NotBlank(message = \"姓名不能为空\")", "@Size(max = 100, message = \"姓名长度不能超过 100 个字符\")",
                        "@NotNull(message = \"性别不能为空\")")
                .doesNotContain("createTime", "tenantId");
        assertThat(files.get("sql/codegen/infra_demo_student.sql"))
                .contains("'infra:demo-student:query'", "'error.code.1099005000'");
        assertThat(files.get("ccloud-module-infra/ccloud-module-infra-api/src/main/java/com/ccloud/module/infra/enums/ErrorCodeConstants_DemoStudent.java.txt"))
                .contains("ErrorCode DEMO_STUDENT_NOT_EXISTS = new ErrorCode(1_099_005_000, \"演示学生不存在\");");
    }

    @Test
    void 树表用户App_列表接口_父节点校验_无权限注解与导出() {
        Map<String, String> files = generate("infra_demo_category", "演示分类表", CATEGORY, CodegenSceneEnum.APP, true);
        String biz = "ccloud-module-infra/ccloud-module-infra-biz/src/main/java/com/ccloud/module/infra/";
        String controller = files.get(biz + "controller/app/demo/AppDemoCategoryController.java");
        assertThat(controller).contains("@RequestMapping(\"/app-api/infra/demo-category\")", "/list")
                .doesNotContain("@PreAuthorize(", "/page", "/export-excel");
        assertThat(files).containsKey(biz + "controller/app/demo/vo/AppDemoCategoryListReqVO.java")
                .doesNotContainKey(biz + "controller/app/demo/vo/AppDemoCategoryPageReqVO.java");
        assertThat(files.get(biz + "service/demo/DemoCategoryServiceImpl.java"))
                .contains("validateParentDemoCategory", "DEMO_CATEGORY_PARENT_IS_CHILD", "selectCountByParentId",
                        "DEMO_CATEGORY_EXISTS_CHILDREN", "DEMO_CATEGORY_PARENT_NOT_EXISTS")
                .doesNotContain("EXITS");
        assertThat(files.get(biz + "dal/dataobject/demo/DemoCategoryDO.java")).contains("extends BaseDO");
        assertThat(files.get("sql/codegen/infra_demo_category.sql")).doesNotContain("system_menu");
    }

    @Test
    void 注释中的引号与反斜杠被转义_生成的Java字符串合法() {
        List<ColumnInfo> columns = new ArrayList<>(CATEGORY);
        columns.set(1, new ColumnInfo("name", "varchar(100)", "varchar", "名称 \"主\" \\ 备", false, null, false, false, 2, 100L));
        Map<String, String> files = generate("infra_demo_category", "演示'分类'表", columns, CodegenSceneEnum.ADMIN, false);
        String respVO = files.get("ccloud-module-infra/ccloud-module-infra-biz/src/main/java/com/ccloud/module/infra/controller/admin/demo/vo/DemoCategoryRespVO.java");
        assertThat(respVO).contains("@Schema(description = \"名称 \\\"主\\\" \\\\ 备\"");
        assertThat(files.get("sql/codegen/infra_demo_category.sql")).contains("'演示''分类'");
    }

    @Test
    void 类描述中的注释结束符与反斜杠被处理_不破坏Java注释与SQL字符串() {
        CodegenTableDO table = builder.buildTable(0L, new TableInfo("infra_demo_category", "演示分类表", List.of(), List.of()))
                .setId(5L).setClassComment("分类*/\\");
        List<CodegenColumnDO> columns = builder.buildColumns(5L, CATEGORY);
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setId(100L + i);
        }
        Map<String, String> files = engine.execute(table, columns);
        assertThat(files.get("ccloud-module-infra/ccloud-module-infra-biz/src/main/java/com/ccloud/module/infra/dal/dataobject/demo/DemoCategoryDO.java"))
                .contains(" * 分类* /&#92; DO").doesNotContain("分类*/");
        // MySQL 字符串中反斜杠是转义符，须加倍，否则结尾的反斜杠会吞掉右引号
        assertThat(files.get("sql/codegen/infra_demo_category.sql"))
                .contains("('分类*/\\\\', ''", "'分类*/\\\\不存在'");
    }

    @Test
    void 树表的父字段或名称字段不在字段中_报错_单表忽略残留的树字段编号() {
        CodegenTableDO table = builder.buildTable(0L, new TableInfo("infra_demo_category", "演示分类表", List.of(), List.of()))
                .setId(5L);
        List<CodegenColumnDO> columns = builder.buildColumns(5L, CATEGORY);
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setId(100L + i);
        }
        Long parentId = findId(columns, "parent_id");
        Long nameId = findId(columns, "name");

        // 单表：残留的、已不存在的树字段编号不影响生成
        table.setTreeParentColumnId(999L).setTreeNameColumnId(998L);
        assertThat(engine.execute(table, columns)).hasSize(11);

        table.setTemplateType(CodegenTemplateTypeEnum.TREE.getType());
        assertTreeColumnRequired(table.setTreeParentColumnId(999L).setTreeNameColumnId(nameId), columns);
        assertTreeColumnRequired(table.setTreeParentColumnId(parentId).setTreeNameColumnId(998L), columns);
        assertTreeColumnRequired(table.setTreeParentColumnId(null).setTreeNameColumnId(nameId), columns);
        // 父字段须为 Long，否则生成的父节点校验无法编译
        assertTreeColumnRequired(table.setTreeParentColumnId(nameId).setTreeNameColumnId(nameId), columns);

        assertThat(engine.execute(table.setTreeParentColumnId(parentId).setTreeNameColumnId(nameId), columns)).hasSize(11);
    }

    @Test
    void 注释与作者中的Unicode转义不能关闭Javadoc_字典类型按Java字符串转义() {
        // javac 在词法分析前翻译 \\uXXXX：\\u002a/ 即 */，不处理反斜杠就能关闭注释、注入代码
        String injected = "名称 \\u002a/ static { System.exit(1); } /\\u002a 备";
        Def d = category();
        d.table().setClassComment(injected).setAuthor("作者 */ \\u002a/");
        d.columns().forEach(c -> c.setColumnComment(injected));
        d.column("name").setDictType("dict\" \\u0022");
        Map<String, String> files = engine.execute(d.table(), d.columns());

        String biz = "ccloud-module-infra/ccloud-module-infra-biz/src/main/java/com/ccloud/module/infra/";
        String dataObject = files.get(biz + "dal/dataobject/demo/DemoCategoryDO.java");
        // DO 中没有字符串字面量需要反斜杠，生成结果中不应出现任何反斜杠
        assertThat(dataObject).doesNotContain("\\", "*/ static")
                .contains(" * 名称 &#92;u002a/ static { System.exit(1); } /&#92;u002a 备 DO", " * @author 作者 * / &#92;u002a/");
        files.forEach((path, code) -> {
            if (path.endsWith(".java")) {
                // 前面有偶数个（含 0 个）反斜杠的 \\u 会被 javac 当作 Unicode 转义
                assertThat(code).as(path).doesNotContainPattern("(?<!\\\\)(?:\\\\\\\\)*\\\\u")
                        .doesNotContain("@author 作者 */");
            }
        });
        assertThat(files.get(biz + "controller/admin/demo/vo/DemoCategoryRespVO.java"))
                .contains("@DictFormat(\"dict\\\" \\\\u0022\")");
    }

    @Test
    void 标识符与字段配置不合法_报配置不合法而不是生成坏代码() {
        assertInvalid(d -> d.table().setBusinessName("../x"));
        assertInvalid(d -> d.table().setModuleName("Infra"));
        assertInvalid(d -> d.table().setModuleName("int"));
        assertInvalid(d -> d.table().setClassName("demoCategory"));
        assertInvalid(d -> d.table().setClassName("Demo;Category"));
        assertInvalid(d -> d.column("name").setJavaField("class"));
        assertInvalid(d -> d.column("name").setJavaField("na-me"));
        assertInvalid(d -> d.column("name").setJavaField("parentId"));
        assertInvalid(d -> d.column("name").setJavaType("Short"));
        assertInvalid(d -> d.column("name").setListOperationCondition("CONTAINS"));
        // LIKE 只能用于字符串：likeIfPresent 的参数是 String
        assertInvalid(d -> d.column("parent_id").setListOperation(true).setListOperationCondition("LIKE"));
        // 树表的父字段须出现在新增/修改 VO 中，生成的父节点校验与测试要用它
        assertInvalid(d -> d.tree().column("parent_id").setUpdateOperation(false));
        assertInvalid(d -> d.tree().column("parent_id").setCreateOperation(false));
        assertThatThrownBy(() -> {
            Def d = category();
            d.column("id").setJavaType("String");
            engine.execute(d.table(), d.columns());
        }).isInstanceOf(ServiceException.class).extracting("code").isEqualTo(CODEGEN_PRIMARY_KEY_REQUIRED.getCode());

        Def ok = category().tree();
        ok.column("name").setListOperationCondition("LIKE");
        assertThat(engine.execute(ok.table(), ok.columns())).hasSize(11);
    }

    private record Def(CodegenTableDO table, List<CodegenColumnDO> columns) {

        CodegenColumnDO column(String name) {
            return columns.stream().filter(c -> c.getColumnName().equals(name)).findFirst().orElseThrow();
        }

        Def tree() {
            table.setTemplateType(CodegenTemplateTypeEnum.TREE.getType())
                    .setTreeParentColumnId(column("parent_id").getId()).setTreeNameColumnId(column("name").getId());
            return this;
        }
    }

    private Def category() {
        CodegenTableDO table = builder.buildTable(0L, new TableInfo("infra_demo_category", "演示分类表", List.of(), List.of()))
                .setId(5L);
        List<CodegenColumnDO> columns = builder.buildColumns(5L, CATEGORY);
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setId(100L + i);
        }
        return new Def(table, columns);
    }

    private void assertInvalid(java.util.function.Consumer<Def> change) {
        Def d = category();
        change.accept(d);
        assertThatThrownBy(() -> engine.execute(d.table(), d.columns()))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(CODEGEN_INVALID_IDENTIFIER.getCode());
    }

    private void assertTreeColumnRequired(CodegenTableDO table, List<CodegenColumnDO> columns) {
        assertThatThrownBy(() -> engine.execute(table, columns))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(CODEGEN_TREE_COLUMN_REQUIRED.getCode());
    }

    private static Long findId(List<CodegenColumnDO> columns, String name) {
        return columns.stream().filter(c -> c.getColumnName().equals(name)).findFirst().orElseThrow().getId();
    }
}

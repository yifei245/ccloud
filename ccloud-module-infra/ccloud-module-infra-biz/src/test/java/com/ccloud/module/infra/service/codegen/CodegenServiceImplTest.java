package com.ccloud.module.infra.service.codegen;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenColumnSaveReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenTableSaveReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenUpdateReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.dal.mysql.codegen.CodegenColumnMapper;
import com.ccloud.module.infra.dal.mysql.codegen.CodegenTableMapper;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.ccloud.module.infra.service.codegen.inner.CodegenBuilder;
import com.ccloud.module.infra.service.codegen.inner.CodegenEngine;
import com.ccloud.module.infra.service.db.DataSourceConfigServiceImpl;
import com.ccloud.module.infra.service.db.DatabaseTableServiceImpl;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Import({CodegenServiceImpl.class, CodegenBuilder.class, DatabaseTableServiceImpl.class, DataSourceConfigServiceImpl.class})
class CodegenServiceImplTest extends BaseInfraDbUnitTest {

    private static final String STUDENT = "infra_demo_student";
    private static final String CATEGORY = "infra_demo_category";

    @Resource
    private CodegenServiceImpl codegenService;
    @Resource
    private CodegenTableMapper codegenTableMapper;
    @Resource
    private CodegenColumnMapper codegenColumnMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    // 引擎在 Task 16 实现，这里替换为桩
    @MockitoBean
    private CodegenEngine codegenEngine;

    private Long importOne(String tableName) {
        return codegenService.createCodegenList(0L, List.of(tableName)).get(0);
    }

    private CodegenColumnDO column(Long tableId, String name) {
        return codegenColumnMapper.selectListByTableId(tableId).stream()
                .filter(c -> c.getColumnName().equals(name)).findFirst().orElseThrow();
    }

    private CodegenUpdateReqVO updateReq(Long tableId) {
        CodegenUpdateReqVO reqVO = new CodegenUpdateReqVO();
        reqVO.setTable(BeanUtils.toBean(codegenTableMapper.selectById(tableId), CodegenTableSaveReqVO.class));
        reqVO.setColumns(BeanUtils.toBean(codegenColumnMapper.selectListByTableId(tableId), CodegenColumnSaveReqVO.class));
        return reqVO;
    }

    @Test
    void createCodegenList_导入演示表_重复导入报错_表不存在报错() {
        Long tableId = importOne(STUDENT);

        CodegenTableDO table = codegenTableMapper.selectById(tableId);
        assertThat(table.getClassName()).isEqualTo("DemoStudent");
        assertThat(table.getBusinessName()).isEqualTo("demo");
        assertThat(table.getModuleName()).isEqualTo("infra");
        assertThat(codegenColumnMapper.selectListByTableId(tableId)).hasSize(11);

        assertServiceException(() -> importOne(STUDENT), CODEGEN_TABLE_EXISTS);
        assertServiceException(() -> importOne("infra_not_exist"), CODEGEN_IMPORT_TABLE_NULL);
        assertServiceException(() -> codegenService.createCodegenList(9999L, List.of(STUDENT)),
                DATA_SOURCE_CONFIG_NOT_EXISTS);
    }

    @Test
    void getDatabaseTableList_排除已导入的表() {
        assertThat(codegenService.getDatabaseTableList(0L, "infra_demo", null))
                .extracting(TableInfo::name).containsExactlyInAnyOrder(STUDENT, CATEGORY);

        importOne(STUDENT);

        assertThat(codegenService.getDatabaseTableList(0L, "infra_demo", null))
                .extracting(TableInfo::name).containsExactly(CATEGORY);
    }

    @Test
    void updateCodegen_树表须指定本表的父字段与名称字段() {
        Long categoryId = importOne(CATEGORY);
        Long studentId = importOne(STUDENT);
        Long parentId = column(categoryId, "parent_id").getId();
        Long nameId = column(categoryId, "name").getId();

        // 不指定父字段
        CodegenUpdateReqVO noParent = updateReq(categoryId);
        noParent.getTable().setTemplateType(CodegenTemplateTypeEnum.TREE.getType());
        noParent.getTable().setTreeNameColumnId(nameId);
        assertServiceException(() -> codegenService.updateCodegen(noParent), CODEGEN_TREE_COLUMN_REQUIRED);

        // 父字段属于别的表
        CodegenUpdateReqVO otherTable = updateReq(categoryId);
        otherTable.getTable().setTemplateType(CodegenTemplateTypeEnum.TREE.getType());
        otherTable.getTable().setTreeParentColumnId(column(studentId, "id").getId());
        otherTable.getTable().setTreeNameColumnId(nameId);
        assertServiceException(() -> codegenService.updateCodegen(otherTable), CODEGEN_TREE_COLUMN_REQUIRED);

        // 父字段不是 Long
        CodegenUpdateReqVO notLong = updateReq(categoryId);
        notLong.getTable().setTemplateType(CodegenTemplateTypeEnum.TREE.getType());
        notLong.getTable().setTreeParentColumnId(nameId);
        notLong.getTable().setTreeNameColumnId(nameId);
        assertServiceException(() -> codegenService.updateCodegen(notLong), CODEGEN_TREE_COLUMN_REQUIRED);

        // 正确指定，字段的修改一并保存
        CodegenUpdateReqVO ok = updateReq(categoryId);
        ok.getTable().setTemplateType(CodegenTemplateTypeEnum.TREE.getType());
        ok.getTable().setTreeParentColumnId(parentId);
        ok.getTable().setTreeNameColumnId(nameId);
        ok.getColumns().stream().filter(c -> c.getId().equals(parentId)).forEach(c -> {
            c.setListOperation(true);
            c.setListOperationCondition("!=");
            c.setDictType("infra_demo_parent");
        });
        codegenService.updateCodegen(ok);

        CodegenTableDO table = codegenTableMapper.selectById(categoryId);
        assertThat(table.getTemplateType()).isEqualTo(CodegenTemplateTypeEnum.TREE.getType());
        assertThat(table.getTreeParentColumnId()).isEqualTo(parentId);
        assertThat(table.getTreeNameColumnId()).isEqualTo(nameId);
        CodegenColumnDO parent = column(categoryId, "parent_id");
        assertThat(parent.getListOperation()).isTrue();
        assertThat(parent.getListOperationCondition()).isEqualTo("!=");
        assertThat(parent.getDictType()).isEqualTo("infra_demo_parent");

        // 提交的字段不属于本表
        CodegenUpdateReqVO foreign = updateReq(categoryId);
        foreign.getColumns().get(0).setId(column(studentId, "id").getId());
        assertServiceException(() -> codegenService.updateCodegen(foreign), CODEGEN_COLUMN_NOT_EXISTS);
    }

    @Test
    void updateCodegen_场景模板类型与查询条件非法时报请求参数错误() {
        Long tableId = importOne(STUDENT);

        CodegenUpdateReqVO badScene = updateReq(tableId);
        badScene.getTable().setScene(99);
        assertBadRequest(badScene);

        CodegenUpdateReqVO badTemplate = updateReq(tableId);
        badTemplate.getTable().setTemplateType(10);
        assertBadRequest(badTemplate);

        CodegenUpdateReqVO badCondition = updateReq(tableId);
        badCondition.getColumns().get(0).setListOperationCondition("CONTAINS");
        assertBadRequest(badCondition);
    }

    private void assertBadRequest(CodegenUpdateReqVO reqVO) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> codegenService.updateCodegen(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code").isEqualTo(BAD_REQUEST.getCode());
    }

    @Test
    void syncCodegenFromDB_新增删除修改的字段分别处理_保留用户编辑() {
        Long tableId = importOne(STUDENT);
        // 模拟：用户编辑了字典；字段定义缺失、过时、多余
        jdbcTemplate.update("UPDATE infra_codegen_column SET dict_type = 'system_user_sex', data_type = 'varchar(50)' "
                + "WHERE table_id = ? AND column_name = 'name'", tableId);
        jdbcTemplate.update("DELETE FROM infra_codegen_column WHERE table_id = ? AND column_name = 'description'", tableId);
        jdbcTemplate.update("INSERT INTO infra_codegen_column (table_id, column_name, data_type, column_comment, nullable, "
                + "primary_key, ordinal_position, java_type, java_field, create_operation, update_operation, "
                + "list_operation, list_operation_condition, list_operation_result, html_type) "
                + "VALUES (?, 'ghost', 'int', '幽灵', 1, 0, 99, 'Integer', 'ghost', 1, 1, 0, '=', 1, 'input')", tableId);

        codegenService.syncCodegenFromDB(tableId);

        List<CodegenColumnDO> columns = codegenColumnMapper.selectListByTableId(tableId);
        assertThat(columns).hasSize(11);
        assertThat(columns).extracting(CodegenColumnDO::getColumnName).contains("description").doesNotContain("ghost");
        CodegenColumnDO name = column(tableId, "name");
        assertThat(name.getDataType()).isEqualTo("varchar(100)");
        assertThat(name.getDictType()).isEqualTo("system_user_sex");

        assertServiceException(() -> codegenService.syncCodegenFromDB(tableId), CODEGEN_SYNC_NONE_CHANGE);
    }

    @Test
    void deleteCodegen_同时删除字段定义() {
        Long tableId = importOne(STUDENT);

        codegenService.deleteCodegen(tableId);

        assertThat(codegenTableMapper.selectById(tableId)).isNull();
        assertThat(codegenColumnMapper.selectListByTableId(tableId)).isEmpty();
        assertServiceException(() -> codegenService.deleteCodegen(tableId), CODEGEN_TABLE_NOT_EXISTS);
    }

    @Test
    void generationCodes_交给引擎生成_表不存在报错() {
        Long tableId = importOne(STUDENT);
        when(codegenEngine.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("a.java", "code"));

        assertThat(codegenService.generationCodes(tableId)).containsEntry("a.java", "code");
        assertServiceException(() -> codegenService.generationCodes(-1L), CODEGEN_TABLE_NOT_EXISTS);
    }
}

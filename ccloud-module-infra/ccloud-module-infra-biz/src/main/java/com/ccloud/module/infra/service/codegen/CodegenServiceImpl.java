package com.ccloud.module.infra.service.codegen;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenColumnSaveReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenTablePageReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenTableSaveReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenUpdateReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.dal.mysql.codegen.CodegenColumnMapper;
import com.ccloud.module.infra.dal.mysql.codegen.CodegenTableMapper;
import com.ccloud.module.infra.enums.codegen.CodegenColumnListConditionEnum;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.ccloud.module.infra.service.codegen.inner.CodegenBuilder;
import com.ccloud.module.infra.service.codegen.inner.CodegenEngine;
import com.ccloud.module.infra.service.db.DataSourceConfigService;
import com.ccloud.module.infra.service.db.DatabaseTableService;
import com.ccloud.module.infra.service.db.dto.TableInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CodegenServiceImpl implements CodegenService {

    @Resource
    private CodegenTableMapper codegenTableMapper;
    @Resource
    private CodegenColumnMapper codegenColumnMapper;
    @Resource
    private DataSourceConfigService dataSourceConfigService;
    @Resource
    private DatabaseTableService databaseTableService;
    @Resource
    private CodegenBuilder codegenBuilder;
    @Resource
    private CodegenEngine codegenEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> createCodegenList(Long dataSourceConfigId, List<String> tableNames) {
        if (dataSourceConfigService.getDataSourceConfig(dataSourceConfigId) == null) {
            throw exception(DATA_SOURCE_CONFIG_NOT_EXISTS);
        }
        List<Long> ids = new ArrayList<>(tableNames.size());
        for (String tableName : tableNames) {
            if (codegenTableMapper.selectByDataSourceConfigIdAndTableName(dataSourceConfigId, tableName) != null) {
                throw exception(CODEGEN_TABLE_EXISTS);
            }
            TableInfo tableInfo = databaseTableService.getTable(dataSourceConfigId, tableName);
            if (tableInfo == null) {
                throw exception(CODEGEN_IMPORT_TABLE_NULL);
            }
            CodegenTableDO table = codegenBuilder.buildTable(dataSourceConfigId, tableInfo);
            codegenTableMapper.insert(table);
            codegenColumnMapper.insertBatch(codegenBuilder.buildColumns(table.getId(), tableInfo.columns()));
            ids.add(table.getId());
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCodegen(CodegenUpdateReqVO reqVO) {
        CodegenTableSaveReqVO tableVO = reqVO.getTable();
        validateCodegenTableExists(tableVO.getId());
        Map<Long, CodegenColumnDO> columns = CollectionUtils.convertMap(
                codegenColumnMapper.selectListByTableId(tableVO.getId()), CodegenColumnDO::getId);
        for (CodegenColumnSaveReqVO columnVO : reqVO.getColumns()) {
            if (!columns.containsKey(columnVO.getId())) {
                throw exception(CODEGEN_COLUMN_NOT_EXISTS);
            }
        }
        if (CodegenTemplateTypeEnum.TREE.getType().equals(tableVO.getTemplateType())) {
            validateTreeColumns(tableVO, columns);
        }
        if (CodegenSceneEnum.valueOf(tableVO.getScene()) == null
                || CodegenTemplateTypeEnum.valueOf(tableVO.getTemplateType()) == null) {
            throw exception(BAD_REQUEST);
        }
        for (CodegenColumnSaveReqVO columnVO : reqVO.getColumns()) {
            if (CodegenColumnListConditionEnum.valueOfCondition(columnVO.getListOperationCondition()) == null) {
                throw exception(BAD_REQUEST);
            }
        }

        codegenTableMapper.updateById(BeanUtils.toBean(tableVO, CodegenTableDO.class));
        for (CodegenColumnSaveReqVO columnVO : reqVO.getColumns()) {
            codegenColumnMapper.updateById(BeanUtils.toBean(columnVO, CodegenColumnDO.class));
        }
    }

    /**
     * 树表须指定本表中类型为 Long 的父编号字段，以及名称字段
     */
    private static void validateTreeColumns(CodegenTableSaveReqVO tableVO, Map<Long, CodegenColumnDO> columns) {
        CodegenColumnDO parent = columns.get(tableVO.getTreeParentColumnId());
        CodegenColumnDO name = columns.get(tableVO.getTreeNameColumnId());
        if (parent == null || name == null || !"Long".equals(parent.getJavaType())) {
            throw exception(CODEGEN_TREE_COLUMN_REQUIRED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncCodegenFromDB(Long tableId) {
        CodegenTableDO table = validateCodegenTableExists(tableId);
        TableInfo tableInfo = databaseTableService.getTable(table.getDataSourceConfigId(), table.getTableName());
        if (tableInfo == null) {
            throw exception(CODEGEN_IMPORT_TABLE_NULL);
        }
        Map<String, CodegenColumnDO> latest = CollectionUtils.convertMap(
                codegenBuilder.buildColumns(tableId, tableInfo.columns()), CodegenColumnDO::getColumnName);
        Map<String, CodegenColumnDO> current = CollectionUtils.convertMap(
                codegenColumnMapper.selectListByTableId(tableId), CodegenColumnDO::getColumnName);

        List<CodegenColumnDO> inserts = latest.values().stream().filter(c -> !current.containsKey(c.getColumnName())).toList();
        List<Long> deletes = current.values().stream().filter(c -> !latest.containsKey(c.getColumnName()))
                .map(CodegenColumnDO::getId).toList();
        // 只更新来自数据库的属性，用户在生成配置里的编辑（用途、字典、示例等）保留
        List<CodegenColumnDO> updates = new ArrayList<>();
        latest.values().stream().filter(c -> current.containsKey(c.getColumnName())).forEach(c -> {
            CodegenColumnDO old = current.get(c.getColumnName());
            if (!Objects.equals(old.getDataType(), c.getDataType()) || !Objects.equals(old.getColumnComment(), c.getColumnComment())
                    || !Objects.equals(old.getNullable(), c.getNullable()) || !Objects.equals(old.getPrimaryKey(), c.getPrimaryKey())
                    || !Objects.equals(old.getOrdinalPosition(), c.getOrdinalPosition())) {
                CodegenColumnDO update = new CodegenColumnDO().setId(old.getId()).setDataType(c.getDataType())
                        .setColumnComment(c.getColumnComment()).setNullable(c.getNullable())
                        .setPrimaryKey(c.getPrimaryKey()).setOrdinalPosition(c.getOrdinalPosition());
                // 只有数据库类型变了才重置 Java 类型，否则保留用户的修改
                if (!Objects.equals(old.getDataType(), c.getDataType())) {
                    update.setJavaType(c.getJavaType());
                }
                updates.add(update);
            }
        });
        if (inserts.isEmpty() && deletes.isEmpty() && updates.isEmpty()) {
            throw exception(CODEGEN_SYNC_NONE_CHANGE);
        }
        if (!inserts.isEmpty()) {
            codegenColumnMapper.insertBatch(inserts);
        }
        if (!deletes.isEmpty()) {
            codegenColumnMapper.deleteByIds(deletes);
        }
        updates.forEach(codegenColumnMapper::updateById);
        codegenTableMapper.updateById(new CodegenTableDO().setId(tableId).setTableComment(tableInfo.comment()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCodegen(Long tableId) {
        validateCodegenTableExists(tableId);
        codegenTableMapper.deleteById(tableId);
        codegenColumnMapper.deleteListByTableId(tableId);
    }

    @Override
    public CodegenTableDO getCodegenTable(Long id) {
        return codegenTableMapper.selectById(id);
    }

    @Override
    public List<CodegenColumnDO> getCodegenColumnListByTableId(Long tableId) {
        return codegenColumnMapper.selectListByTableId(tableId);
    }

    @Override
    public PageResult<CodegenTableDO> getCodegenTablePage(CodegenTablePageReqVO reqVO) {
        return codegenTableMapper.selectPage(reqVO);
    }

    @Override
    public List<TableInfo> getDatabaseTableList(Long dataSourceConfigId, String name, String comment) {
        Set<String> imported = CollectionUtils.convertSet(
                codegenTableMapper.selectListByDataSourceConfigId(dataSourceConfigId), CodegenTableDO::getTableName);
        return databaseTableService.getTableList(dataSourceConfigId, name, comment).stream()
                .filter(table -> !imported.contains(table.name())).toList();
    }

    @Override
    public Map<String, String> generationCodes(Long tableId) {
        CodegenTableDO table = validateCodegenTableExists(tableId);
        return codegenEngine.execute(table, codegenColumnMapper.selectListByTableId(tableId));
    }

    private CodegenTableDO validateCodegenTableExists(Long id) {
        CodegenTableDO table = codegenTableMapper.selectById(id);
        if (table == null) {
            throw exception(CODEGEN_TABLE_NOT_EXISTS);
        }
        return table;
    }
}

package com.ccloud.module.infra.service.codegen;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenTablePageReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenUpdateReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.service.db.dto.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * 代码生成：表与字段定义的导入、编辑、同步，以及代码生成
 */
public interface CodegenService {

    /**
     * 导入数据库中的表，按表结构推断默认的生成配置
     *
     * @return 表定义编号列表
     */
    List<Long> createCodegenList(Long dataSourceConfigId, List<String> tableNames);

    /**
     * 修改表定义与字段定义
     */
    void updateCodegen(CodegenUpdateReqVO reqVO);

    /**
     * 按数据库当前表结构同步字段定义：新增、删除、更新来自数据库的属性，保留用户在生成配置里的编辑
     */
    void syncCodegenFromDB(Long tableId);

    /**
     * 删除表定义及其字段定义
     */
    void deleteCodegen(Long tableId);

    /**
     * @return 表定义；不存在返回 null
     */
    CodegenTableDO getCodegenTable(Long id);

    /**
     * @return 字段定义，按 ordinalPosition 升序
     */
    List<CodegenColumnDO> getCodegenColumnListByTableId(Long tableId);

    PageResult<CodegenTableDO> getCodegenTablePage(CodegenTablePageReqVO reqVO);

    /**
     * 数据源中尚未导入的表，按表名、表注释模糊筛选
     */
    List<TableInfo> getDatabaseTableList(Long dataSourceConfigId, String name, String comment);

    /**
     * 生成代码
     *
     * @return 文件路径 → 内容，按生成顺序
     */
    Map<String, String> generationCodes(Long tableId);
}

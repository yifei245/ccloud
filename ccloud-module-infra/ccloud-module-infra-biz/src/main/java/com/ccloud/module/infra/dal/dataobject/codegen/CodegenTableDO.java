package com.ccloud.module.infra.dal.dataobject.codegen;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 代码生成的表定义。全局数据，所有租户共享
 */
@TableName("infra_codegen_table")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class CodegenTableDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 数据源配置编号
     */
    private Long dataSourceConfigId;
    /**
     * 生成场景：1 管理后台 2 用户 App
     */
    private Integer scene;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 表描述
     */
    private String tableComment;
    /**
     * 备注
     */
    private String remark;
    /**
     * 模块名
     */
    private String moduleName;
    /**
     * 业务名
     */
    private String businessName;
    /**
     * 类名
     */
    private String className;
    /**
     * 类描述
     */
    private String classComment;
    /**
     * 作者
     */
    private String author;
    /**
     * 模板类型：1 单表 2 树表 10 主表 11 子表
     */
    private Integer templateType;
    /**
     * 前端类型
     */
    private Integer frontType;
    /**
     * 父菜单编号
     */
    private Long parentMenuId;
    /**
     * 主表编号，子表生成时使用（预留）
     */
    private Long masterTableId;
    /**
     * 子表关联主表的字段编号（预留）
     */
    private Long subJoinColumnId;
    /**
     * 主表与子表是否一对多（预留）
     */
    private Boolean subJoinMany;
    /**
     * 树表的父字段编号
     */
    private Long treeParentColumnId;
    /**
     * 树表的名字字段编号
     */
    private Long treeNameColumnId;
}

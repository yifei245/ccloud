package com.ccloud.module.infra.dal.dataobject.codegen;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 代码生成的字段定义。全局数据，所有租户共享
 */
@TableName("infra_codegen_column")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class CodegenColumnDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 表编号
     */
    private Long tableId;
    /**
     * 字段名
     */
    private String columnName;
    /**
     * 字段类型，完整类型，如 varchar(100)
     */
    private String dataType;
    /**
     * 字段描述
     */
    private String columnComment;
    /**
     * 是否允许为空
     */
    private Boolean nullable;
    /**
     * 是否主键
     */
    private Boolean primaryKey;
    /**
     * 排序
     */
    private Integer ordinalPosition;
    /**
     * Java 属性类型
     */
    private String javaType;
    /**
     * Java 属性名
     */
    private String javaField;
    /**
     * 字典类型
     */
    private String dictType;
    /**
     * 数据示例
     */
    private String example;
    /**
     * 是否为新增操作的字段
     */
    private Boolean createOperation;
    /**
     * 是否为更新操作的字段
     */
    private Boolean updateOperation;
    /**
     * 是否为查询条件字段
     */
    private Boolean listOperation;
    /**
     * 查询条件类型，对应 CodegenColumnListConditionEnum
     */
    private String listOperationCondition;
    /**
     * 是否为列表结果字段
     */
    private Boolean listOperationResult;
    /**
     * 显示类型，对应 CodegenColumnHtmlTypeEnum
     */
    private String htmlType;
}

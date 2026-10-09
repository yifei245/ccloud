package com.ccloud.module.system.dal.dataobject.dict;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;


/**
 * 字典类型，全局共享
 */
@TableName("system_dict_type")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class DictTypeDO extends BaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 类型标识，字典数据通过它关联；创建后不可修改
     */
    private String type;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    private String remark;
}

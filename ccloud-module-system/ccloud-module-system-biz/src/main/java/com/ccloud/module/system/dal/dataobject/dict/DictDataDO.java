package com.ccloud.module.system.dal.dataobject.dict;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 字典数据，全局共享
 */
@TableName("system_dict_data")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class DictDataDO extends BaseDO {

    @TableId
    private Long id;
    private Integer sort;
    private String label;
    private String value;
    private String dictType;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 前端标签颜色：default、primary、success、info、warning、danger
     */
    private String colorType;
    private String cssClass;
    private String remark;
}

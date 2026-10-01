package com.ccloud.module.infra.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 文件存储配置。全局数据，所有租户共享
 */
@TableName("infra_file_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "config")
@Accessors(chain = true)
public class FileConfigDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 配置名
     */
    private String name;
    /**
     * 存储器，见 FileStorageEnum
     */
    private Integer storage;
    /**
     * 备注
     */
    private String remark;
    /**
     * 是否为主配置，全局只有一个
     */
    private Boolean master;
    /**
     * 存储配置，JSON，含访问密钥
     */
    private String config;
}

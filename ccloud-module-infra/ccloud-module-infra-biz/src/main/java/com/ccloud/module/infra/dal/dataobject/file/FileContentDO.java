package com.ccloud.module.infra.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 文件内容，仅数据库存储器使用。全局数据
 */
@TableName("infra_file_content")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "content")
@Accessors(chain = true)
public class FileContentDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 配置编号
     */
    private Long configId;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 文件内容
     */
    private byte[] content;
}

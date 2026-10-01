package com.ccloud.module.infra.dal.dataobject.file;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 文件。每次上传记一条；相同内容在同一存储里只存一份，由多条记录共同引用
 */
@TableName("infra_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class FileDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 配置编号，即文件存放在哪个存储
     */
    private Long configId;
    /**
     * 原始文件名
     */
    private String name;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 访问地址
     */
    private String url;
    /**
     * MIME 类型，依据内容识别
     */
    private String type;
    /**
     * 文件大小，单位字节
     */
    private Integer size;
}

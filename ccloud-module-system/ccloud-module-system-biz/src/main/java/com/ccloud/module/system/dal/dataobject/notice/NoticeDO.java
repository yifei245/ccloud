package com.ccloud.module.system.dal.dataobject.notice;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 通知公告，按租户隔离
 */
@TableName("system_notice")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class NoticeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String title;
    /**
     * 内容，富文本
     */
    private String content;
    /**
     * 类型，见 NoticeTypeEnum
     */
    private Integer type;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
}

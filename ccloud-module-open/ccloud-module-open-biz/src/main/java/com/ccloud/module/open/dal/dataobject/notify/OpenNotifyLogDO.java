package com.ccloud.module.open.dal.dataobject.notify;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.module.open.enums.notify.OpenNotifyStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 开放应用事件回调日志。租户编号为应用所属的租户，由发送方按应用写入；失败按退避策略重试
 */
@TableName("open_notify_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OpenNotifyLogDO extends TenantBaseDO {

    /**
     * 日志编号
     */
    @TableId
    private Long id;
    /**
     * 应用编号
     */
    private Long appId;
    /**
     * 事件编号，回调时放入 X-Open-Event-Id，供对方去重
     */
    private String eventId;
    /**
     * 事件类型，如 open.test
     */
    private String event;
    /**
     * 回调请求体，JSON，签名即对它计算
     */
    private String payload;
    /**
     * 回调地址，每次发送时更新为应用当前的地址
     */
    private String notifyUrl;
    /**
     * 状态，见 {@link OpenNotifyStatusEnum}
     */
    private Integer status;
    /**
     * 已重试次数
     */
    private Integer retryCount;
    /**
     * 下次重试时间；发送中为抢占租约的结束时间
     */
    private LocalDateTime nextRetryTime;
    /**
     * 最近一次通知时间
     */
    private LocalDateTime lastNotifyTime;
    /**
     * 最近一次对方返回的 HTTP 状态码，请求未发出时为 0
     */
    private Integer httpStatus;
    /**
     * 对方最近一次响应内容，截断到 2048 字符；未发出时为失败原因
     */
    private String responseBody;
}

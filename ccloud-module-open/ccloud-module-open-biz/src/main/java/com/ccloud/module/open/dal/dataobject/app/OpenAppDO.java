package com.ccloud.module.open.dal.dataobject.app;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.framework.mybatis.core.type.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 开放应用。按租户隔离；第三方凭 appKey + appSecret 调用 /open-api
 */
@TableName(value = "open_app", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "appSecret")
@Accessors(chain = true)
public class OpenAppDO extends TenantBaseDO {

    /**
     * 应用编号
     */
    @TableId
    private Long id;
    /**
     * 应用名称
     */
    private String name;
    /**
     * 应用标识，系统生成，全局唯一，不可修改
     */
    private String appKey;
    /**
     * 应用密钥，库中为密文；读取后为原文，只在新建、重置时返回给调用方
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String appSecret;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * IP 白名单，逗号分隔的地址或 CIDR，为空表示不限制
     */
    private String ipWhitelist;
    /**
     * 每秒请求上限，网关按应用令牌桶限流
     */
    private Integer rateLimit;
    /**
     * 过期时间，为空表示长期有效
     */
    private LocalDateTime expireTime;
    /**
     * 联系人
     */
    private String contactName;
    /**
     * 联系电话
     */
    private String contactMobile;
    /**
     * 事件回调地址
     */
    private String notifyUrl;
}

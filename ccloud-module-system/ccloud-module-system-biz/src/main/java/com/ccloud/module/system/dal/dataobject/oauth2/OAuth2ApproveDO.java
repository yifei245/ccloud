package com.ccloud.module.system.dal.dataobject.oauth2;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户对客户端授权范围的同意记录，一个范围一条
 */
@TableName("system_oauth2_approve")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class OAuth2ApproveDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    private Integer userType;
    private String clientId;
    private String scope;
    private Boolean approved;
    private LocalDateTime expiresTime;
}

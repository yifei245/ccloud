package com.ccloud.framework.security.core;

import com.ccloud.framework.common.enums.UserTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 登录用户。
 *
 * <p>由网关解析令牌后签名下发，业务服务验签后还原。它是<b>令牌内容的镜像</b>，
 * 不含任何需要回查数据库的字段——一旦需要查库，网关的缓存就失去意义。
 */
@Data
public class LoginUser implements Serializable {

    /**
     * 用户编号。在 {@link #userType} 限定的体系内唯一，跨体系可能重复
     */
    private Long id;

    /**
     * 用户类型，见 {@link UserTypeEnum}
     */
    private Integer userType;

    /**
     * 附加信息（昵称、部门编号等）。只放展示与日志用得上的少量字段
     */
    private Map<String, String> info;

    private Long tenantId;

    /**
     * 令牌授权范围。开放 API 的权限控制依据
     */
    private List<String> scopes;

    /**
     * 签发该身份的客户端，用于区分终端
     */
    private String clientId;

    /**
     * 令牌过期时间。业务侧会二次校验，网关缓存期内被吊销的令牌由此兜住
     */
    private LocalDateTime expiresTime;

    public boolean isExpired() {
        return expiresTime != null && expiresTime.isBefore(LocalDateTime.now());
    }
}

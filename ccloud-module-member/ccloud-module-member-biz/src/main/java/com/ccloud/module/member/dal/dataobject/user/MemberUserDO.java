package com.ccloud.module.member.dal.dataobject.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 会员用户。与管理员 system_users 是两套独立体系，同一手机号互不相干
 */
@TableName("member_user")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "password")
@Accessors(chain = true)
public class MemberUserDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 手机号，租户内唯一，是会员的登录账号
     */
    private String mobile;
    /**
     * BCrypt 哈希，任何接口都不得返回。短信或小程序注册的用户为空串，只能用验证码登录直到设置密码
     */
    @JsonIgnore
    private String password;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    private String registerIp;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.TerminalEnum}
     */
    private Integer registerTerminal;
    private String loginIp;
    private LocalDateTime loginDate;

    // ========== 基本信息 ==========

    private String nickname;
    private String avatar;
    private String name;
    /**
     * 0 未知 1 男 2 女
     */
    private Integer sex;
    private LocalDateTime birthday;
    private Integer areaId;
    /**
     * 运营备注，仅管理后台可见
     */
    private String mark;

    // ========== 积分、等级、分组（P2 商城使用） ==========

    private Integer point;
    /**
     * 标签编号，逗号分隔
     */
    private String tagIds;
    private Long levelId;
    private Integer experience;
    private Long groupId;
}

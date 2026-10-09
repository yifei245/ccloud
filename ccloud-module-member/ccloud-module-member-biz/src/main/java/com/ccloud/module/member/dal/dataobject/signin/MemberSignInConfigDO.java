package com.ccloud.module.member.dal.dataobject.signin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 签到规则：连续签到第几天奖励多少积分与经验。天数在租户内唯一
 */
@TableName("member_sign_in_config")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberSignInConfigDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 连续签到第几天，从 1 开始
     */
    private Integer day;
    private Integer point;
    private Integer experience;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
}

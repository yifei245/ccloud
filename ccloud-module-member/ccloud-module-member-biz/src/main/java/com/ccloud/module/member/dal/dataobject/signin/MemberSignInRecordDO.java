package com.ccloud.module.member.dal.dataobject.signin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 签到记录。签到日期取创建时间
 */
@TableName("member_sign_in_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberSignInRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 本次是连续签到的第几天
     */
    private Integer day;
    /**
     * 本次获得的积分
     */
    private Integer point;
    /**
     * 本次获得的经验
     */
    private Integer experience;
}

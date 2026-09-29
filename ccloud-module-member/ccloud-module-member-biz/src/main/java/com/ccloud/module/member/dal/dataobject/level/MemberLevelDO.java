package com.ccloud.module.member.dal.dataobject.level;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员等级。会员的等级由经验决定：取门槛经验不超过会员经验的最高一个启用等级
 */
@TableName("member_level")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberLevelDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String name;
    /**
     * 等级序号，数值越大等级越高，租户内唯一
     */
    private Integer level;
    /**
     * 升级所需经验。序号越大门槛越高
     */
    private Integer experience;
    /**
     * 享受折扣百分比，100 表示不打折
     */
    private Integer discountPercent;
    private String icon;
    private String backgroundUrl;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
}

package com.ccloud.module.member.dal.dataobject.level;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员等级变更记录。保存变更时新等级的快照：等级配置之后可能被修改，记录应反映当时的状态
 */
@TableName("member_level_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberLevelRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 新等级编号。变为无等级时为 0
     */
    private Long levelId;
    private Integer level;
    private Integer discountPercent;
    /**
     * 新等级的升级所需经验
     */
    private Integer experience;
    /**
     * 变更时会员的经验
     */
    private Integer userExperience;
    /**
     * 管理员修改等级时填写的原因；经验变动引起的变更为空串
     */
    private String remark;
    /**
     * 如「升级为白银会员」「降级为青铜会员」
     */
    private String description;
}

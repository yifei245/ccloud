package com.ccloud.module.member.dal.dataobject.level;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员经验记录。「会员 + 业务类型 + 业务编号」已有记录即表示该业务已处理，重复调用不再生效
 */
@TableName("member_experience_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberExperienceRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 枚举 {@link com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum}
     */
    private Integer bizType;
    private String bizId;
    private String title;
    private String description;
    /**
     * 实际变动的经验，负数表示扣减
     */
    private Integer experience;
    /**
     * 变动后的总经验
     */
    private Integer totalExperience;
}

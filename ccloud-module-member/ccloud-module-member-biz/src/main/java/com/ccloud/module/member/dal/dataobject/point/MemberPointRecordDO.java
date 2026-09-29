package com.ccloud.module.member.dal.dataobject.point;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员积分记录。「会员 + 业务类型 + 业务编号」已有记录即表示该业务已处理，重复调用不再生效
 */
@TableName("member_point_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberPointRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    private String bizId;
    /**
     * 枚举 {@link com.ccloud.module.member.enums.point.MemberPointBizTypeEnum}
     */
    private Integer bizType;
    private String title;
    private String description;
    /**
     * 变动的积分，负数表示扣减
     */
    private Integer point;
    /**
     * 变动后的积分余额
     */
    private Integer totalPoint;
}

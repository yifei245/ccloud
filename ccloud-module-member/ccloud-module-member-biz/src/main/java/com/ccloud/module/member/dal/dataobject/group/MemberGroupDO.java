package com.ccloud.module.member.dal.dataobject.group;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员分组。每个会员至多属于一个分组
 */
@TableName("member_group")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberGroupDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 名称，租户内唯一
     */
    private String name;
    private String remark;
    /**
     * 枚举 {@link com.ccloud.framework.common.enums.CommonStatusEnum}。停用的分组不能再分配给会员
     */
    private Integer status;
}

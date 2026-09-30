package com.ccloud.module.member.dal.dataobject.tag;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员标签。会员可打多个标签，编号以逗号分隔存于 member_user.tag_ids
 */
@TableName("member_tag")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberTagDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 名称，租户内唯一
     */
    private String name;
}

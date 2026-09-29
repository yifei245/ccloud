package com.ccloud.module.system.dal.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户与岗位关联。
 *
 * <p>与 system_users.post_ids 冗余存储：post_ids 便于展示，本表便于"按岗位查用户"
 */
@TableName("system_user_post")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class UserPostDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    private Long postId;
}

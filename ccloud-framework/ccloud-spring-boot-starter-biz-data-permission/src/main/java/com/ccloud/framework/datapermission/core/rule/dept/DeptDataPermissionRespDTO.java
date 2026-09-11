package com.ccloud.framework.datapermission.core.rule.dept;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 用户的部门维度数据权限。
 *
 * <p>三个字段是"或"的关系：{@code all} 为真时其余忽略；否则可见范围是
 * {@code deptIds} 对应的部门数据，并入 {@code self} 为真时的本人数据。
 */
@Data
@Accessors(chain = true)
public class DeptDataPermissionRespDTO implements Serializable {

    /**
     * 是否可查看全部数据
     */
    private Boolean all = Boolean.FALSE;

    /**
     * 可查看的部门编号集合
     */
    private Set<Long> deptIds = Collections.emptySet();

    /**
     * 是否可查看本人数据
     */
    private Boolean self = Boolean.FALSE;

    public boolean isAll() {
        return Boolean.TRUE.equals(all);
    }

    public boolean isSelf() {
        return Boolean.TRUE.equals(self);
    }
}

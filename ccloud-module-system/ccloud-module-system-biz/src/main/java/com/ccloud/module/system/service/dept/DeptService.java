package com.ccloud.module.system.service.dept;

import com.ccloud.module.system.dal.dataobject.dept.DeptDO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 部门 Service
 */
public interface DeptService {

    DeptDO getDept(Long id);

    List<DeptDO> getDeptList(Collection<Long> ids);

    /**
     * @return 全部层级的子孙部门，不含自身
     */
    List<DeptDO> getChildDeptList(Long id);

    /**
     * @return 自身及全部子孙部门的编号
     */
    Set<Long> getSelfAndChildDeptIdList(Long id);

    /**
     * 校验部门均存在且开启；为空时不校验
     */
    void validateDeptList(Collection<Long> ids);
}

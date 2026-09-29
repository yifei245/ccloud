package com.ccloud.module.system.service.dept;

import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 部门 Service
 */
public interface DeptService {

    Long createDept(@Valid DeptSaveReqVO createReqVO);

    void updateDept(@Valid DeptSaveReqVO updateReqVO);

    /**
     * 存在子部门时拒绝
     */
    void deleteDept(Long id);

    void deleteDeptList(List<Long> ids);

    DeptDO getDept(Long id);

    List<DeptDO> getDeptList(Collection<Long> ids);

    List<DeptDO> getDeptList(DeptListReqVO reqVO);

    /**
     * @return 全部层级的子孙部门，不含自身
     */
    List<DeptDO> getChildDeptList(Long id);

    /**
     * @return 全部层级的子孙部门编号，不含自身；部门结构变更时整体失效
     */
    Set<Long> getChildDeptIdListFromCache(Long id);

    /**
     * @return 自身及全部子孙部门的编号
     */
    Set<Long> getSelfAndChildDeptIdList(Long id);

    /**
     * 校验部门均存在且开启；为空时不校验
     */
    void validateDeptList(Collection<Long> ids);
}

package com.ccloud.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.mysql.dept.DeptMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.DEPT_NOT_ENABLE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.DEPT_NOT_FOUND;

@Service
@Validated
@Slf4j
public class DeptServiceImpl implements DeptService {

    /**
     * 部门层级的安全上限，防止脏数据形成环时无限循环
     */
    private static final int MAX_DEPTH = Short.MAX_VALUE;

    @Resource
    private DeptMapper deptMapper;

    @Override
    public DeptDO getDept(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    public List<DeptDO> getDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return deptMapper.selectByIds(ids);
    }

    /**
     * 按层广度遍历，每层一次查询
     */
    @Override
    public List<DeptDO> getChildDeptList(Long id) {
        List<DeptDO> children = new ArrayList<>();
        Set<Long> visited = new HashSet<>(Collections.singleton(id));
        Collection<Long> parentIds = Collections.singleton(id);
        for (int depth = 0; depth < MAX_DEPTH && !parentIds.isEmpty(); depth++) {
            List<DeptDO> depts = deptMapper.selectListByParentId(parentIds);
            List<DeptDO> fresh = CollectionUtils.filterList(depts, dept -> visited.add(dept.getId()));
            children.addAll(fresh);
            parentIds = CollectionUtils.convertSet(fresh, DeptDO::getId);
        }
        return children;
    }

    @Override
    public Set<Long> getSelfAndChildDeptIdList(Long id) {
        Set<Long> ids = CollectionUtils.convertSet(getChildDeptList(id), DeptDO::getId);
        ids.add(id);
        return ids;
    }

    @Override
    public void validateDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Map<Long, DeptDO> deptMap = CollectionUtils.convertMap(deptMapper.selectByIds(ids), DeptDO::getId);
        for (Long id : ids) {
            DeptDO dept = deptMap.get(id);
            if (dept == null) {
                throw exception(DEPT_NOT_FOUND);
            }
            if (!CommonStatusEnum.isEnable(dept.getStatus())) {
                throw exception(DEPT_NOT_ENABLE, dept.getName());
            }
        }
    }
}

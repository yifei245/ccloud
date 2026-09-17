package com.ccloud.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.util.collection.CollectionUtils;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.mysql.dept.DeptMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.dal.redis.RedisKeyConstants.DEPT_CHILDREN_ID_LIST;
import static com.ccloud.module.system.enums.ErrorCodeConstants.*;

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
    /**
     * 自身代理：带缓存注解的方法在类内调用时必须经过代理才生效
     */
    @Resource
    @Lazy
    private DeptService self;

    // ========== 增删改 ==========

    /**
     * 新增、移动、删除部门都会改变某些部门的子孙集合，且无法低成本定位受影响的键，统一整体失效
     */
    @Override
    @CacheEvict(value = DEPT_CHILDREN_ID_LIST, allEntries = true)
    public Long createDept(DeptSaveReqVO createReqVO) {
        normalizeParentId(createReqVO);
        validateParentDept(null, createReqVO.getParentId());
        validateDeptNameUnique(null, createReqVO.getParentId(), createReqVO.getName());

        DeptDO dept = BeanUtils.toBean(createReqVO, DeptDO.class);
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @CacheEvict(value = DEPT_CHILDREN_ID_LIST, allEntries = true)
    public void updateDept(DeptSaveReqVO updateReqVO) {
        normalizeParentId(updateReqVO);
        validateDeptExists(updateReqVO.getId());
        validateParentDept(updateReqVO.getId(), updateReqVO.getParentId());
        validateDeptNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());

        deptMapper.updateById(BeanUtils.toBean(updateReqVO, DeptDO.class));
    }

    @Override
    @CacheEvict(value = DEPT_CHILDREN_ID_LIST, allEntries = true)
    public void deleteDept(Long id) {
        validateDeptExists(id);
        if (deptMapper.selectCountByParentId(id) > 0) {
            throw exception(DEPT_EXITS_CHILDREN);
        }
        deptMapper.deleteById(id);
    }

    /**
     * 选中的部门中，子孙先于祖先删除，使同时选中父子部门的批量删除可以成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = DEPT_CHILDREN_ID_LIST, allEntries = true)
    public void deleteDeptList(List<Long> ids) {
        Map<Long, DeptDO> all = CollectionUtils.convertMap(deptMapper.selectList(), DeptDO::getId);
        Map<Long, Integer> depth = new HashMap<>();
        ids.forEach(id -> depth.put(id, depthOf(all.get(id), all)));
        ids.stream().sorted(Comparator.comparing((Long id) -> depth.get(id)).reversed())
                .forEach(this::deleteDept);
    }

    private static int depthOf(DeptDO dept, Map<Long, DeptDO> all) {
        int depth = 0;
        DeptDO current = dept;
        while (current != null && !DeptDO.PARENT_ID_ROOT.equals(current.getParentId()) && depth < all.size()) {
            current = all.get(current.getParentId());
            depth++;
        }
        return depth;
    }

    private static void normalizeParentId(DeptSaveReqVO reqVO) {
        if (reqVO.getParentId() == null) {
            reqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
    }

    private void validateDeptExists(Long id) {
        if (id == null || deptMapper.selectById(id) == null) {
            throw exception(DEPT_NOT_FOUND);
        }
    }

    /**
     * 父部门须存在；不能是自己，也不能是自己的子孙（否则形成环）
     */
    private void validateParentDept(Long id, Long parentId) {
        if (DeptDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        if (parentId.equals(id)) {
            throw exception(DEPT_PARENT_ERROR);
        }
        DeptDO parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw exception(DEPT_PARENT_NOT_EXITS);
        }
        if (id == null) {
            return;
        }
        // 自下而上沿父部门回溯，遇到自身即成环；直接查库而不用缓存，保证校验基于最新结构
        for (int i = 0; i < MAX_DEPTH && parent != null; i++) {
            if (id.equals(parent.getId())) {
                throw exception(DEPT_PARENT_IS_CHILD);
            }
            if (DeptDO.PARENT_ID_ROOT.equals(parent.getParentId())) {
                return;
            }
            parent = deptMapper.selectById(parent.getParentId());
        }
    }

    /**
     * 同一父部门下名称唯一
     */
    private void validateDeptNameUnique(Long id, Long parentId, String name) {
        DeptDO dept = deptMapper.selectByParentIdAndName(parentId, name);
        if (dept != null && !dept.getId().equals(id)) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
    }

    // ========== 查询 ==========

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

    @Override
    public List<DeptDO> getDeptList(DeptListReqVO reqVO) {
        return deptMapper.selectList(reqVO);
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
    @Cacheable(value = DEPT_CHILDREN_ID_LIST, key = "#id")
    public Set<Long> getChildDeptIdListFromCache(Long id) {
        return CollectionUtils.convertSet(getChildDeptList(id), DeptDO::getId);
    }

    @Override
    public Set<Long> getSelfAndChildDeptIdList(Long id) {
        Set<Long> ids = new HashSet<>(self.getChildDeptIdListFromCache(id));
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

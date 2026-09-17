package com.ccloud.module.system.service.dept;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.mysql.dept.DeptMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.DEPT_NOT_ENABLE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.DEPT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

@Import(DeptServiceImpl.class)
class DeptServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private DeptServiceImpl deptService;
    @Resource
    private DeptMapper deptMapper;

    @Test
    void getChildDeptList_返回全部层级的子孙部门_不含自身() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        DeptDO a = insert("A", root.getId(), CommonStatusEnum.ENABLE);
        DeptDO a1 = insert("A1", a.getId(), CommonStatusEnum.ENABLE);
        DeptDO a11 = insert("A11", a1.getId(), CommonStatusEnum.ENABLE);
        insert("其他", 0L, CommonStatusEnum.ENABLE);

        assertThat(deptService.getChildDeptList(root.getId())).extracting(DeptDO::getId)
                .containsExactlyInAnyOrder(a.getId(), a1.getId(), a11.getId());
        assertThat(deptService.getChildDeptList(a11.getId())).isEmpty();
    }

    @Test
    void validateDeptList_三个分支() {
        DeptDO ok = insert("开启", 0L, CommonStatusEnum.ENABLE);
        DeptDO disabled = insert("停用", 0L, CommonStatusEnum.DISABLE);

        deptService.validateDeptList(List.of(ok.getId()));
        deptService.validateDeptList(List.of());
        assertServiceException(() -> deptService.validateDeptList(List.of(-1L)), DEPT_NOT_FOUND);
        assertServiceException(() -> deptService.validateDeptList(List.of(disabled.getId())), DEPT_NOT_ENABLE);
    }

    @Test
    void getDept与getDeptList() {
        DeptDO a = insert("甲", 0L, CommonStatusEnum.ENABLE);
        DeptDO b = insert("乙", 0L, CommonStatusEnum.ENABLE);
        assertThat(deptService.getDept(a.getId()).getName()).isEqualTo("甲");
        assertThat(deptService.getDeptList(List.of(a.getId(), b.getId()))).hasSize(2);
    }

    private DeptDO insert(String name, Long parentId, CommonStatusEnum status) {
        DeptDO dept = new DeptDO().setName(name).setParentId(parentId).setSort(0).setStatus(status.getStatus());
        deptMapper.insert(dept);
        return dept;
    }
}

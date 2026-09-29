package com.ccloud.module.system.service.dept;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import com.ccloud.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.ccloud.module.system.dal.dataobject.dept.DeptDO;
import com.ccloud.module.system.dal.mysql.dept.DeptMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.ccloud.module.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import({DeptServiceImpl.class, CacheTestConfiguration.class})
class DeptServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private DeptService deptService;
    @Resource
    private DeptMapper deptMapper;

    // ========== 创建 ==========

    @Test
    void createDept_成功() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        Long id = deptService.createDept(newReqVO("研发部", root.getId()));

        DeptDO dept = deptMapper.selectById(id);
        assertThat(dept.getName()).isEqualTo("研发部");
        assertThat(dept.getParentId()).isEqualTo(root.getId());
        assertThat(dept.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createDept_父部门为空时作为顶级() {
        DeptSaveReqVO reqVO = newReqVO("顶级", null);
        Long id = deptService.createDept(reqVO);
        assertThat(deptMapper.selectById(id).getParentId()).isEqualTo(DeptDO.PARENT_ID_ROOT);
    }

    @Test
    void createDept_父部门不存在() {
        assertServiceException(() -> deptService.createDept(newReqVO("研发部", -1L)), DEPT_PARENT_NOT_EXITS);
    }

    @Test
    void createDept_同级重名失败_不同级允许同名() {
        DeptDO a = insert("A", 0L, CommonStatusEnum.ENABLE);
        DeptDO b = insert("B", 0L, CommonStatusEnum.ENABLE);
        deptService.createDept(newReqVO("财务部", a.getId()));

        assertServiceException(() -> deptService.createDept(newReqVO("财务部", a.getId())), DEPT_NAME_DUPLICATE);
        assertThat(deptService.createDept(newReqVO("财务部", b.getId()))).isNotNull();
    }

    // ========== 修改 ==========

    @Test
    void updateDept_成功() {
        DeptDO dept = insert("旧名", 0L, CommonStatusEnum.ENABLE);
        DeptSaveReqVO reqVO = newReqVO("新名", 0L);
        reqVO.setId(dept.getId());
        deptService.updateDept(reqVO);
        assertThat(deptMapper.selectById(dept.getId()).getName()).isEqualTo("新名");
    }

    @Test
    void updateDept_不存在() {
        DeptSaveReqVO reqVO = newReqVO("x", 0L);
        reqVO.setId(-1L);
        assertServiceException(() -> deptService.updateDept(reqVO), DEPT_NOT_FOUND);
    }

    @Test
    void updateDept_父部门设为自身失败() {
        DeptDO dept = insert("自身", 0L, CommonStatusEnum.ENABLE);
        DeptSaveReqVO reqVO = newReqVO("自身", dept.getId());
        reqVO.setId(dept.getId());
        assertServiceException(() -> deptService.updateDept(reqVO), DEPT_PARENT_ERROR);
    }

    @Test
    void updateDept_父部门设为自己的子孙部门失败() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        DeptDO child = insert("分部", root.getId(), CommonStatusEnum.ENABLE);
        DeptDO grandChild = insert("小组", child.getId(), CommonStatusEnum.ENABLE);

        DeptSaveReqVO reqVO = newReqVO("总部", grandChild.getId());
        reqVO.setId(root.getId());
        assertServiceException(() -> deptService.updateDept(reqVO), DEPT_PARENT_IS_CHILD);
    }

    @Test
    void updateDept_移动部门后子孙缓存随之更新() {
        DeptDO a = insert("A", 0L, CommonStatusEnum.ENABLE);
        DeptDO b = insert("B", 0L, CommonStatusEnum.ENABLE);
        DeptDO child = insert("子", a.getId(), CommonStatusEnum.ENABLE);
        assertThat(deptService.getChildDeptIdListFromCache(a.getId())).containsExactly(child.getId());

        DeptSaveReqVO reqVO = newReqVO("子", b.getId());
        reqVO.setId(child.getId());
        deptService.updateDept(reqVO);

        assertThat(deptService.getChildDeptIdListFromCache(a.getId())).isEmpty();
        assertThat(deptService.getChildDeptIdListFromCache(b.getId())).containsExactly(child.getId());
    }

    // ========== 删除 ==========

    @Test
    void deleteDept_成功() {
        DeptDO dept = insert("待删", 0L, CommonStatusEnum.ENABLE);
        deptService.deleteDept(dept.getId());
        assertThat(deptMapper.selectById(dept.getId())).isNull();
    }

    @Test
    void deleteDept_有子部门时失败() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        insert("分部", root.getId(), CommonStatusEnum.ENABLE);
        assertServiceException(() -> deptService.deleteDept(root.getId()), DEPT_EXITS_CHILDREN);
    }

    @Test
    void deleteDept_不存在() {
        assertServiceException(() -> deptService.deleteDept(-1L), DEPT_NOT_FOUND);
    }

    @Test
    void deleteDeptList_可同时删除父子部门() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        DeptDO child = insert("分部", root.getId(), CommonStatusEnum.ENABLE);
        deptService.deleteDeptList(List.of(root.getId(), child.getId()));
        assertThat(deptMapper.selectCount()).isZero();
    }

    // ========== 查询 ==========

    @Test
    void getChildDeptList与缓存版本_返回全部层级的子孙部门_不含自身() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        DeptDO a = insert("A", root.getId(), CommonStatusEnum.ENABLE);
        DeptDO a1 = insert("A1", a.getId(), CommonStatusEnum.ENABLE);
        DeptDO a11 = insert("A11", a1.getId(), CommonStatusEnum.ENABLE);
        insert("其他", 0L, CommonStatusEnum.ENABLE);

        assertThat(deptService.getChildDeptList(root.getId())).extracting(DeptDO::getId)
                .containsExactlyInAnyOrder(a.getId(), a1.getId(), a11.getId());
        assertThat(deptService.getChildDeptIdListFromCache(root.getId()))
                .containsExactlyInAnyOrder(a.getId(), a1.getId(), a11.getId());
        assertThat(deptService.getChildDeptList(a11.getId())).isEmpty();
        assertThat(deptService.getSelfAndChildDeptIdList(a.getId()))
                .containsExactlyInAnyOrder(a.getId(), a1.getId(), a11.getId());
    }

    @Test
    void getChildDeptIdListFromCache_新增子部门后缓存失效() {
        DeptDO root = insert("总部", 0L, CommonStatusEnum.ENABLE);
        assertThat(deptService.getChildDeptIdListFromCache(root.getId())).isEmpty();

        Long childId = deptService.createDept(newReqVO("新分部", root.getId()));

        assertThat(deptService.getChildDeptIdListFromCache(root.getId())).containsExactly(childId);
    }

    @Test
    void getDeptList_按名称与状态过滤() {
        insert("华东销售", 0L, CommonStatusEnum.ENABLE);
        insert("华南销售", 0L, CommonStatusEnum.DISABLE);
        insert("研发", 0L, CommonStatusEnum.ENABLE);

        DeptListReqVO reqVO = new DeptListReqVO();
        reqVO.setName("销售");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        assertThat(deptService.getDeptList(reqVO)).extracting(DeptDO::getName).containsExactly("华东销售");
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

    private static DeptSaveReqVO newReqVO(String name, Long parentId) {
        DeptSaveReqVO reqVO = new DeptSaveReqVO();
        reqVO.setName(name);
        reqVO.setParentId(parentId);
        reqVO.setSort(1);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}

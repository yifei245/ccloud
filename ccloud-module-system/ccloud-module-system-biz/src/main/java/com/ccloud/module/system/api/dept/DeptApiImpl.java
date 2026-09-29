package com.ccloud.module.system.api.dept;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.dept.dto.DeptRespDTO;
import com.ccloud.module.system.service.dept.DeptService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class DeptApiImpl implements DeptApi {

    @Resource
    private DeptService deptService;

    @Override
    public CommonResult<DeptRespDTO> getDept(Long id) {
        return success(BeanUtils.toBean(deptService.getDept(id), DeptRespDTO.class));
    }

    @Override
    public CommonResult<List<DeptRespDTO>> getDeptList(Collection<Long> ids) {
        return success(BeanUtils.toBean(deptService.getDeptList(ids), DeptRespDTO.class));
    }

    @Override
    public CommonResult<List<DeptRespDTO>> getChildDeptList(Long id) {
        return success(BeanUtils.toBean(deptService.getChildDeptList(id), DeptRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> validateDeptList(Collection<Long> ids) {
        deptService.validateDeptList(ids);
        return success(true);
    }
}

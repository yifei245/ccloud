package com.ccloud.module.system.controller.admin.dict;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypePageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypeRespVO;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypeSaveReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.type.DictTypeSimpleRespVO;
import com.ccloud.module.system.dal.dataobject.dict.DictTypeDO;
import com.ccloud.module.system.service.dict.DictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.ccloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.ccloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 字典类型")
@RestController
@RequestMapping("/admin-api/system/dict-type")
@Validated
public class DictTypeController {

    @Resource
    private DictTypeService dictTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建字典类型")
    @PreAuthorize("@ss.hasPermission('system:dict:create')")
    public CommonResult<Long> createDictType(@Valid @RequestBody DictTypeSaveReqVO createReqVO) {
        return success(dictTypeService.createDictType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改字典类型", description = "类型标识创建后不可修改")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    public CommonResult<Boolean> updateDictType(@Valid @RequestBody DictTypeSaveReqVO updateReqVO) {
        dictTypeService.updateDictType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除字典类型", description = "存在字典数据时不允许删除")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    public CommonResult<Boolean> deleteDictType(@RequestParam("id") Long id) {
        dictTypeService.deleteDictType(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除字典类型")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    public CommonResult<Boolean> deleteDictTypeList(@RequestParam("ids") List<Long> ids) {
        dictTypeService.deleteDictTypeList(ids);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得字典类型的分页列表")
    @PreAuthorize("@ss.hasPermission('system:dict:query')")
    public CommonResult<PageResult<DictTypeRespVO>> pageDictTypes(@Valid DictTypePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(dictTypeService.getDictTypePage(pageReqVO), DictTypeRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "查询字典类型详细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dict:query')")
    public CommonResult<DictTypeRespVO> getDictType(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(dictTypeService.getDictType(id), DictTypeRespVO.class));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得全部字典类型列表", description = "包括开启与停用的，主要用于前端的下拉选项")
    public CommonResult<List<DictTypeSimpleRespVO>> getSimpleDictTypeList() {
        return success(BeanUtils.toBean(dictTypeService.getDictTypeList(), DictTypeSimpleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出字典类型")
    @PreAuthorize("@ss.hasPermission('system:dict:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(HttpServletResponse response, @Valid DictTypePageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DictTypeDO> list = dictTypeService.getDictTypePage(exportReqVO).getList();
        ExcelUtils.write(response, "字典类型.xls", "数据", DictTypeRespVO.class, BeanUtils.toBean(list, DictTypeRespVO.class));
    }
}

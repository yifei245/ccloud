package com.ccloud.module.system.controller.admin.dict;

import com.ccloud.framework.apilog.core.annotation.ApiAccessLog;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataPageReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataRespVO;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import com.ccloud.module.system.controller.admin.dict.vo.data.DictDataSimpleRespVO;
import com.ccloud.module.system.dal.dataobject.dict.DictDataDO;
import com.ccloud.module.system.service.dict.DictDataService;
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

@Tag(name = "管理后台 - 字典数据")
@RestController
@RequestMapping("/admin-api/system/dict-data")
@Validated
public class DictDataController {

    @Resource
    private DictDataService dictDataService;

    @PostMapping("/create")
    @Operation(summary = "新增字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:create')")
    public CommonResult<Long> createDictData(@Valid @RequestBody DictDataSaveReqVO createReqVO) {
        return success(dictDataService.createDictData(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    public CommonResult<Boolean> updateDictData(@Valid @RequestBody DictDataSaveReqVO updateReqVO) {
        dictDataService.updateDictData(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除字典数据")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    public CommonResult<Boolean> deleteDictData(@RequestParam("id") Long id) {
        dictDataService.deleteDictData(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除字典数据")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    public CommonResult<Boolean> deleteDictDataList(@RequestParam("ids") List<Long> ids) {
        dictDataService.deleteDictDataList(ids);
        return success(true);
    }

    /**
     * 登录页也需要（性别、终端等下拉框），免登录，见 SecurityConfiguration
     */
    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得全部字典数据列表", description = "只包含开启的字典数据，一般用于前端缓存字典")
    public CommonResult<List<DictDataSimpleRespVO>> getSimpleDictDataList() {
        List<DictDataDO> list = dictDataService.getDictDataList(CommonStatusEnum.ENABLE.getStatus(), null);
        return success(BeanUtils.toBean(list, DictDataSimpleRespVO.class));
    }

    @GetMapping("/type")
    @Operation(summary = "根据字典类型查询字典数据", description = "只包含开启的字典数据")
    @Parameter(name = "type", description = "字典类型", required = true, example = "system_user_sex")
    public CommonResult<List<DictDataSimpleRespVO>> getDictDataListByType(@RequestParam("type") String type) {
        List<DictDataDO> list = dictDataService.getDictDataList(CommonStatusEnum.ENABLE.getStatus(), type);
        return success(BeanUtils.toBean(list, DictDataSimpleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得字典数据的分页列表")
    @PreAuthorize("@ss.hasPermission('system:dict:query')")
    public CommonResult<PageResult<DictDataRespVO>> getDictTypePage(@Valid DictDataPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(dictDataService.getDictDataPage(pageReqVO), DictDataRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "查询字典数据详细")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dict:query')")
    public CommonResult<DictDataRespVO> getDictData(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(dictDataService.getDictData(id), DictDataRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(HttpServletResponse response, @Valid DictDataPageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DictDataDO> list = dictDataService.getDictDataPage(exportReqVO).getList();
        ExcelUtils.write(response, "字典数据.xls", "数据", DictDataRespVO.class, BeanUtils.toBean(list, DictDataRespVO.class));
    }
}

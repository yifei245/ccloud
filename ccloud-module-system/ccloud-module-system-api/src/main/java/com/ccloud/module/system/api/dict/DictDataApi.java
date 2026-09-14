package com.ccloud.module.system.api.dict;

import com.ccloud.framework.common.pojo.CommonResult;
import com.ccloud.module.system.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ccloud.module.system.api.dict.dto.DictDataRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据 RPC。excel 导入导出的字典翻译与各模块的取值校验依赖它
 */
@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 字典数据")
public interface DictDataApi {

    String PREFIX = ApiConstants.PREFIX + "/dict-data";

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验字典值是否均有效")
    CommonResult<Boolean> validateDictDataList(@RequestParam("dictType") String dictType,
                                               @RequestParam("values") Collection<String> values);

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "按值获得字典数据，未命中返回 null")
    CommonResult<DictDataRespDTO> getDictData(@RequestParam("dictType") String dictType,
                                              @RequestParam("value") String value);

    @GetMapping(PREFIX + "/parse")
    @Operation(summary = "按标签反查字典数据，未命中返回 null")
    CommonResult<DictDataRespDTO> parseDictData(@RequestParam("dictType") String dictType,
                                                @RequestParam("label") String label);

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得指定类型的全部字典数据")
    CommonResult<List<DictDataRespDTO>> getDictDataList(@RequestParam("dictType") String dictType);
}

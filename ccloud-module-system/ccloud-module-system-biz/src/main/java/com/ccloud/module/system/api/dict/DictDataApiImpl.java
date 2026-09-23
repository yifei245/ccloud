package com.ccloud.module.system.api.dict;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.api.dict.dto.DictDataRespDTO;
import com.ccloud.module.system.service.dict.DictDataService;
import com.ccloud.framework.common.pojo.CommonResult;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static com.ccloud.framework.common.pojo.CommonResult.success;

/**
 * 字典 RPC 实现：其他服务的 Excel 字典转换器与参数校验依赖它
 */
@RestController
@Validated
public class DictDataApiImpl implements DictDataApi {

    @Resource
    private DictDataService dictDataService;

    @Override
    public CommonResult<Boolean> validateDictDataList(String dictType, Collection<String> values) {
        dictDataService.validateDictDataList(dictType, values);
        return success(true);
    }

    @Override
    public CommonResult<DictDataRespDTO> getDictData(String dictType, String value) {
        return success(BeanUtils.toBean(dictDataService.getDictData(dictType, value), DictDataRespDTO.class));
    }

    @Override
    public CommonResult<DictDataRespDTO> parseDictData(String dictType, String label) {
        return success(BeanUtils.toBean(dictDataService.parseDictData(dictType, label), DictDataRespDTO.class));
    }

    @Override
    public CommonResult<List<DictDataRespDTO>> getDictDataList(String dictType) {
        return success(BeanUtils.toBean(dictDataService.getDictDataListByDictType(dictType), DictDataRespDTO.class));
    }
}

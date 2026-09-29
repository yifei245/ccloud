package com.ccloud.module.member.framework.contract;

import com.ccloud.framework.excel.core.service.DictFrameworkService;
import com.ccloud.module.system.api.dict.DictDataApi;
import com.ccloud.module.system.api.dict.dto.DictDataRespDTO;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Excel 导入导出的字典翻译，经 RPC 由 system 提供
 */
@Component
public class DictFrameworkServiceImpl implements DictFrameworkService {

    @Lazy
    @Resource
    private DictDataApi dictDataApi;

    @Override
    public String getDictDataLabel(String dictType, String value) {
        DictDataRespDTO data = dictDataApi.getDictData(dictType, value).getCheckedData();
        return data != null ? data.getLabel() : null;
    }

    @Override
    public String getDictDataValue(String dictType, String label) {
        DictDataRespDTO data = dictDataApi.parseDictData(dictType, label).getCheckedData();
        return data != null ? data.getValue() : null;
    }
}

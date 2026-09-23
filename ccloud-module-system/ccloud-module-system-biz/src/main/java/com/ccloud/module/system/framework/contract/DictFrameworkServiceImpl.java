package com.ccloud.module.system.framework.contract;

import com.ccloud.framework.excel.core.service.DictFrameworkService;
import com.ccloud.module.system.service.dict.DictDataService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Excel 导入导出的字典翻译
 */
@Component
public class DictFrameworkServiceImpl implements DictFrameworkService {

    /**
     * 延迟注入：框架在初始化 MyBatis 拦截器时就要创建本类，而这些 Service 依赖 Mapper，
     * 直接注入会形成循环依赖。首次调用时才真正取得 Service
     */
    @Lazy
    @Resource
    private DictDataService dictDataService;

    @Override
    public String getDictDataLabel(String dictType, String value) {
        return dictDataService.getDictDataLabel(dictType, value);
    }

    @Override
    public String getDictDataValue(String dictType, String label) {
        return dictDataService.parseDictDataValue(dictType, label);
    }
}

package com.ccloud.framework.excel.config;

import com.ccloud.framework.excel.core.convert.DictConvert;
import com.ccloud.framework.excel.core.service.DictFrameworkService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Excel 自动配置。
 *
 * <p>把字典服务注入 {@link DictConvert} 的静态字段——FastExcel 的转换器由框架
 * 反射实例化，拿不到 Spring 容器，只能这样桥接。
 */
@Slf4j
@AutoConfiguration
public class CcloudExcelAutoConfiguration {

    private final ObjectProvider<DictFrameworkService> dictFrameworkServiceProvider;

    public CcloudExcelAutoConfiguration(ObjectProvider<DictFrameworkService> dictFrameworkServiceProvider) {
        this.dictFrameworkServiceProvider = dictFrameworkServiceProvider;
    }

    /**
     * 字典服务由 system 模块经 RPC 提供，未接入时字典列导出原始码值
     */
    @PostConstruct
    public void init() {
        DictFrameworkService service = dictFrameworkServiceProvider.getIfAvailable();
        DictConvert.setDictFrameworkService(service);
        if (service == null) {
            log.info("[init] 未接入字典服务，Excel 中的字典列将导出原始码值");
        }
    }
}

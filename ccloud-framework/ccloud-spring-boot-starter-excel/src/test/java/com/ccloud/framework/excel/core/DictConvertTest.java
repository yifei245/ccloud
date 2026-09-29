package com.ccloud.framework.excel.core;

import cn.idev.excel.metadata.data.WriteCellData;
import com.ccloud.framework.excel.core.convert.DictConvert;
import com.ccloud.framework.excel.core.service.DictFrameworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DictConvertTest {

    private DictFrameworkService dictService;

    @BeforeEach
    void setUp() {
        dictService = mock(DictFrameworkService.class);
        DictConvert.setDictFrameworkService(dictService);
    }

    @Test
    @DisplayName("导出时字典值应被翻译成标签")
    void valueToLabel_shouldTranslate() {
        when(dictService.getDictDataLabel("common_status", "0")).thenReturn("开启");
        assertThat(DictConvert.translateToLabel("common_status", "0")).isEqualTo("开启");
    }

    @Test
    @DisplayName("导入时字典标签应被还原成值")
    void labelToValue_shouldTranslateBack() {
        when(dictService.getDictDataValue("common_status", "开启")).thenReturn("0");
        assertThat(DictConvert.translateToValue("common_status", "开启")).isEqualTo("0");
    }

    @Test
    @DisplayName("字典中查不到时返回原值，导出不该因为一条脏数据整体失败")
    void unknownValue_shouldFallbackToOriginal() {
        when(dictService.getDictDataLabel("common_status", "9")).thenReturn(null);
        assertThat(DictConvert.translateToLabel("common_status", "9")).isEqualTo("9");
    }

    @Test
    @DisplayName("未注册字典服务时直接返回原值而非空指针")
    void withoutService_shouldReturnOriginal() {
        DictConvert.setDictFrameworkService(null);
        assertThat(DictConvert.translateToLabel("common_status", "0")).isEqualTo("0");
    }
}

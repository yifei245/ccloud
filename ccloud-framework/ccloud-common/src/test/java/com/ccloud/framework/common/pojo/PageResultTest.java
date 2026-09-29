package com.ccloud.framework.common.pojo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    @DisplayName("empty 应返回空列表与 0 总数")
    void empty_shouldReturnEmptyListAndZeroTotal() {
        PageResult<String> p = PageResult.empty();
        assertThat(p.getList()).isEmpty();
        assertThat(p.getTotal()).isZero();
    }

    @Test
    @DisplayName("empty(total) 应返回空列表但保留总数")
    void emptyWithTotal_shouldKeepTotal() {
        PageResult<String> p = PageResult.empty(42L);
        assertThat(p.getList()).isEmpty();
        assertThat(p.getTotal()).isEqualTo(42L);
    }

    @Test
    @DisplayName("构造后 list 与 total 应可正常读取")
    void constructor_shouldSetListAndTotal() {
        PageResult<String> p = new PageResult<>(List.of("a", "b"), 10L);
        assertThat(p.getList()).containsExactly("a", "b");
        assertThat(p.getTotal()).isEqualTo(10L);
    }

    @Test
    @DisplayName("PageParam 默认值应为第 1 页、每页 10 条")
    void pageParam_shouldHaveSaneDefaults() {
        PageParam param = new PageParam();
        assertThat(param.getPageNo()).isEqualTo(1);
        assertThat(param.getPageSize()).isEqualTo(10);
        assertThat(PageParam.PAGE_SIZE_NONE).isEqualTo(-1);
    }
}

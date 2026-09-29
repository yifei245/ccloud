package com.ccloud.framework.mybatis.core.type;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JsonLongSetTypeHandlerTest {

    private final JsonLongSetTypeHandler handler = new JsonLongSetTypeHandler();

    /**
     * 必须写成数字数组：与初始化脚本的 [1,2] 格式一致，MySQL 的 JSON_CONTAINS(menu_ids, '1') 才能命中。
     * 全局 JSON 配置为前端精度会把 Long 序列化为字符串，不能直接复用。
     */
    @Test
    void 写入时应序列化为数字数组() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 1, new LinkedHashSet<>(List.of(3L, 1L, 9007199254740993L)), null);
        verify(ps).setString(1, "[3,1,9007199254740993]");
    }

    @Test
    void 读取时兼容数字数组_字符串数组与空值() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("a")).thenReturn("[1,2]");
        when(rs.getString("b")).thenReturn("[\"3\",\"4\"]");
        when(rs.getString("c")).thenReturn("");

        assertThat(handler.getNullableResult(rs, "a")).containsExactly(1L, 2L);
        assertThat(handler.getNullableResult(rs, "b")).containsExactly(3L, 4L);
        assertThat(handler.getNullableResult(rs, "c")).isEmpty();
    }
}

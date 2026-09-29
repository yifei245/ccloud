package com.ccloud.framework.mybatis.core.type;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JSON 数组 &lt;-&gt; {@code Set<Long>} 的类型处理器。
 *
 * <p>直接继承 {@link BaseTypeHandler} 而非 MP 的 {@code AbstractJsonTypeHandler}：
 * 后者的构造签名在 MP 各版本间变动过，直接实现更稳定。
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(Set.class)
public class JsonLongSetTypeHandler extends BaseTypeHandler<Set<Long>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<Long> parameter, JdbcType jdbcType)
            throws SQLException {
        // 手工拼接数字数组：全局 JSON 配置会把 Long 写成字符串（为前端精度），存库时要保持 [1,2] 格式
        StringBuilder json = new StringBuilder("[");
        for (Long value : parameter) {
            if (value == null) {
                continue;
            }
            if (json.length() > 1) {
                json.append(',');
            }
            json.append(value);
        }
        ps.setString(i, json.append(']').toString());
    }

    @Override
    public Set<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Set<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Set<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    /**
     * 解析失败时返回空集合而非抛异常——历史脏数据不该让整个查询挂掉
     */
    private Set<Long> parse(String json) {
        if (StrUtil.isBlank(json)) {
            return new LinkedHashSet<>();
        }
        Set<Long> result = JsonUtils.parseObjectQuietly(json, new TypeReference<LinkedHashSet<Long>>() {
        });
        return result == null ? new LinkedHashSet<>() : result;
    }
}

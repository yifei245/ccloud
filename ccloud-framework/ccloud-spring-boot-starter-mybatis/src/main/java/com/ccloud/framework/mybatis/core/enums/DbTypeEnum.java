package com.ccloud.framework.mybatis.core.enums;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库类型。P1 只支持 MySQL，保留枚举是为了让 IdType 推断与后续扩展有明确落点。
 */
@Getter
@AllArgsConstructor
public enum DbTypeEnum {

    MY_SQL("MySQL", DbType.MYSQL);

    private final String database;
    private final DbType mpDbType;

    private static final Map<String, DbTypeEnum> MAP_BY_JDBC_PROTOCOL =
            Map.of("mysql", MY_SQL);

    public static DbTypeEnum find(String jdbcProtocol) {
        return jdbcProtocol == null ? null : MAP_BY_JDBC_PROTOCOL.get(jdbcProtocol.toLowerCase());
    }

    public static DbType findByJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> jdbcUrl.toLowerCase().contains(":" + e.database.toLowerCase() + ":"))
                .map(DbTypeEnum::getMpDbType)
                .findFirst().orElse(null);
    }
}

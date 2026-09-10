package com.ccloud.framework.tenant.core.db;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ccloud.framework.tenant.config.TenantProperties;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户条件生成器。决定"哪些表要拼 tenant_id"以及"拼什么值"。
 */
@Slf4j
public class TenantLineHandlerImpl implements TenantLineHandler {

    private static final String TENANT_ID_COLUMN = "tenant_id";

    /**
     * 全部小写的忽略表名，用于大小写不敏感匹配
     */
    private final Set<String> ignoreTables;

    public TenantLineHandlerImpl(TenantProperties properties) {
        this.ignoreTables = properties.getIgnoreTables().stream()
                .map(table -> table.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContextHolder.getRequiredTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_ID_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 显式忽略：系统级操作
        if (TenantContextHolder.isIgnore()) {
            return true;
        }
        // 无租户上下文时不拼条件。若强行拼，会生成 tenant_id = null 这种恒不成立的条件，
        // 表现为"查不到任何数据"，排查起来比直接不过滤更费劲
        if (TenantContextHolder.getTenantId() == null) {
            return true;
        }
        return ignoreTables.contains(tableName.toLowerCase(Locale.ROOT));
    }
}

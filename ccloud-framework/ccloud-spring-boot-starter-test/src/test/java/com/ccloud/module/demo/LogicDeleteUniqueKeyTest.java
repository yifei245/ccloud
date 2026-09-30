package com.ccloud.module.demo;

import com.ccloud.framework.test.core.ut.BaseDbUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 逻辑删除与唯一索引：正式表的唯一索引都带 deleted 列（如 infra_config 的 uk_config_key (config_key, deleted)）。
 * 删除时 deleted 写入记录自身的编号，同一个值可以反复"创建、删除"，而未删除的记录之间仍受唯一约束
 */
class LogicDeleteUniqueKeyTest extends BaseDbUnitTest {

    @Autowired
    private DemoConfigMapper configMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 同一唯一值可以反复创建与删除() {
        for (int i = 0; i < 3; i++) {
            DemoConfigDO config = config("demo.key");
            configMapper.insert(config);
            configMapper.deleteById(config.getId());
        }
        assertThat(configMapper.selectList(DemoConfigDO::getConfigKey, "demo.key")).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM infra_config WHERE config_key = 'demo.key'",
                Integer.class)).isEqualTo(3);
    }

    @Test
    void 删除后deleted为记录编号_未删除为0() {
        DemoConfigDO kept = config("demo.kept");
        DemoConfigDO removed = config("demo.removed");
        configMapper.insert(kept);
        configMapper.insert(removed);
        configMapper.deleteById(removed.getId());

        assertThat(deletedOf(kept.getId())).isZero();
        assertThat(deletedOf(removed.getId())).isEqualTo(removed.getId());
    }

    @Test
    void 未删除的记录之间仍受唯一约束() {
        configMapper.insert(config("demo.unique"));
        assertThatThrownBy(() -> configMapper.insert(config("demo.unique"))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void 批量删除同样写入各自编号() {
        DemoConfigDO a = config("demo.batch");
        configMapper.insert(a);
        configMapper.deleteByIds(List.of(a.getId()));
        DemoConfigDO b = config("demo.batch");
        configMapper.insert(b);
        configMapper.delete(DemoConfigDO::getConfigKey, "demo.batch");

        assertThat(deletedOf(a.getId())).isEqualTo(a.getId());
        assertThat(deletedOf(b.getId())).isEqualTo(b.getId());
    }

    private Long deletedOf(Long id) {
        return jdbcTemplate.queryForObject("SELECT deleted FROM infra_config WHERE id = ?", Long.class, id);
    }

    private static DemoConfigDO config(String key) {
        return new DemoConfigDO().setCategory("demo").setType(2).setName("演示").setConfigKey(key).setValue("v")
                .setVisible(true);
    }
}

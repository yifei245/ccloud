package com.ccloud.module.demo;

import com.ccloud.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseMapperXBatchTest extends BaseDbUnitTest {

    @Resource
    private DemoPostMapper demoPostMapper;

    /**
     * insertBatch 走独立的批量执行器；若 MyBatis 一级缓存为 SESSION 级，
     * 同一事务内再次执行相同查询会命中批量写入之前的缓存结果，读到旧数据。
     */
    @Test
    void 同一事务内批量插入后再次查询应读到新数据() {
        assertThat(demoPostMapper.selectList(DemoPostDO::getCode, "batch")).isEmpty();

        demoPostMapper.insertBatch(List.of(
                new DemoPostDO().setCode("batch").setName("甲").setSort(1).setStatus(0).setTenantId(1L),
                new DemoPostDO().setCode("batch").setName("乙").setSort(2).setStatus(0).setTenantId(1L)));

        assertThat(demoPostMapper.selectList(DemoPostDO::getCode, "batch"))
                .extracting(DemoPostDO::getName).containsExactlyInAnyOrder("甲", "乙");
    }
}

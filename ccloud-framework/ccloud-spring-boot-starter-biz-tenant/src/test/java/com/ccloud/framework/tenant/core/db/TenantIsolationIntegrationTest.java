package com.ccloud.framework.tenant.core.db;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多租户隔离的端到端验证。
 *
 * <p>这组测试不看拦截器是否被注册，只看<b>查询结果是否真的被隔离</b>。
 * 多租户最危险的故障形态是"SQL 照常执行、不报错，但条件没拼上去"——
 * 例如 JSqlParser 版本与 MyBatis Plus 不匹配时就会这样。断言注册关系抓不到它，
 * 断言查询结果可以。
 */
@SpringBootTest(classes = TenantIsolationIntegrationTest.TestApp.class)
@ActiveProfiles("unit-test")
class TenantIsolationIntegrationTest {

    /**
     * 刻意不手工装配拦截器：忽略表等配置全部走 application-unit-test.yaml，
     * 拦截器由 CcloudTenantAutoConfiguration + CcloudMybatisAutoConfiguration 自动装配。
     * 这样测的才是生产环境真正跑的那条链路，包括拦截器之间的先后顺序。
     */
    @Configuration
    @EnableAutoConfiguration
    @MapperScan("com.ccloud.framework.tenant.core.db")
    static class TestApp {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @TableName("demo_tenant")
    public static class DemoTenantDO extends TenantBaseDO {
        private Long id;
        private String name;
    }

    @Mapper
    public interface DemoTenantMapper extends BaseMapperX<DemoTenantDO> {
    }

    @Autowired
    private DemoTenantMapper mapper;

    @BeforeEach
    void seed() {
        TenantUtils.executeIgnore(() -> mapper.delete(new LambdaQueryWrapperX<DemoTenantDO>()));
        TenantUtils.execute(1L, () -> {
            mapper.insert(newDemo("租户1-甲"));
            mapper.insert(newDemo("租户1-乙"));
        });
        TenantUtils.execute(2L, () -> mapper.insert(newDemo("租户2-丙")));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private DemoTenantDO newDemo(String name) {
        DemoTenantDO demo = new DemoTenantDO();
        demo.setName(name);
        return demo;
    }

    @Test
    @DisplayName("插入时应自动写入当前租户编号，无需业务代码手工赋值")
    void insert_shouldPopulateTenantIdAutomatically() {
        List<DemoTenantDO> all = TenantUtils.executeIgnore(() -> mapper.selectList());
        assertThat(all).hasSize(3);
        assertThat(all).extracting(DemoTenantDO::getTenantId).containsExactlyInAnyOrder(1L, 1L, 2L);
    }

    @Test
    @DisplayName("查询只应返回当前租户的数据")
    void selectList_shouldOnlyReturnCurrentTenantRows() {
        TenantContextHolder.setTenantId(1L);
        assertThat(mapper.selectList()).extracting(DemoTenantDO::getName)
                .containsExactlyInAnyOrder("租户1-甲", "租户1-乙");

        TenantContextHolder.setTenantId(2L);
        assertThat(mapper.selectList()).extracting(DemoTenantDO::getName)
                .containsExactly("租户2-丙");
    }

    @Test
    @DisplayName("count 也应被隔离——聚合查询同样会泄漏数据量信息")
    void selectCount_shouldBeIsolated() {
        TenantContextHolder.setTenantId(1L);
        assertThat(mapper.selectCount()).isEqualTo(2L);
        TenantContextHolder.setTenantId(2L);
        assertThat(mapper.selectCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("分页查询的 list 与 total 都应被隔离")
    void selectPage_shouldIsolateBothListAndTotal() {
        TenantContextHolder.setTenantId(1L);
        PageResult<DemoTenantDO> page = mapper.selectPage(new PageParam(),
                new LambdaQueryWrapperX<DemoTenantDO>());
        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getList()).hasSize(2);
    }

    @Test
    @DisplayName("按主键查询他租户的数据应查不到，防止越权读取")
    void selectById_shouldNotCrossTenantBoundary() {
        Long otherTenantRowId = TenantUtils.execute(2L,
                () -> mapper.selectList().get(0).getId());

        TenantContextHolder.setTenantId(1L);
        assertThat(mapper.selectById(otherTenantRowId)).isNull();
    }

    @Test
    @DisplayName("更新他租户的数据应影响 0 行，防止越权修改")
    void update_shouldNotAffectOtherTenantRows() {
        DemoTenantDO otherRow = TenantUtils.execute(2L, () -> mapper.selectList().get(0));

        TenantContextHolder.setTenantId(1L);
        DemoTenantDO evil = new DemoTenantDO();
        evil.setId(otherRow.getId());
        evil.setName("被篡改");
        assertThat(mapper.updateById(evil)).isZero();

        DemoTenantDO after = TenantUtils.execute(2L, () -> mapper.selectById(otherRow.getId()));
        assertThat(after.getName()).isEqualTo("租户2-丙");
    }

    @Test
    @DisplayName("删除他租户的数据应影响 0 行")
    void delete_shouldNotAffectOtherTenantRows() {
        DemoTenantDO otherRow = TenantUtils.execute(2L, () -> mapper.selectList().get(0));

        TenantContextHolder.setTenantId(1L);
        assertThat(mapper.deleteById(otherRow.getId())).isZero();
        assertThat(TenantUtils.execute(2L, () -> mapper.selectCount())).isEqualTo(1L);
    }

    @Test
    @DisplayName("忽略租户时应能读到全部数据，供系统级任务使用")
    void executeIgnore_shouldSeeAllTenants() {
        assertThat(TenantUtils.executeIgnore(() -> mapper.selectCount())).isEqualTo(3L);
    }

    @Test
    @DisplayName("TenantUtils.execute 结束后应恢复原有租户上下文，而不是简单清空")
    void execute_shouldRestorePreviousTenantAfterwards() {
        TenantContextHolder.setTenantId(1L);
        TenantUtils.execute(2L, () -> mapper.selectCount());
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(1L);
    }
}

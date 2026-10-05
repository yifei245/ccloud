package com.ccloud.module.infra.service.db;

import com.ccloud.framework.test.core.ut.LocalMySql;
import com.ccloud.module.infra.controller.admin.db.vo.DataSourceConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.dal.dataobject.db.DataSourceConfigDO;
import com.ccloud.module.infra.dal.mysql.codegen.CodegenTableMapper;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(DataSourceConfigServiceImpl.class)
class DataSourceConfigServiceImplTest extends BaseInfraDbUnitTest {

    @Resource
    private DataSourceConfigServiceImpl dataSourceConfigService;
    @Resource
    private CodegenTableMapper codegenTableMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void getDataSourceConfigList_主数据源在首位且取自应用配置() {
        Long id = dataSourceConfigService.createDataSourceConfig(save(null, "ut-ds"));

        var list = dataSourceConfigService.getDataSourceConfigList();

        assertThat(list.get(0).getId()).isEqualTo(0L);
        assertThat(list.get(0).getName()).isEqualTo("master");
        assertThat(list.get(0).getUrl()).isEqualTo(LocalMySql.jdbcUrl());
        assertThat(list).extracting(DataSourceConfigDO::getId).contains(id);
    }

    @Test
    void createDataSourceConfig_连接成功才保存_密码加密存储() {
        Long id = dataSourceConfigService.createDataSourceConfig(save(null, "ut-ds"));

        String stored = jdbcTemplate.queryForObject(
                "SELECT password FROM infra_data_source_config WHERE id = ?", String.class, id);
        assertThat(stored).isNotEqualTo(LocalMySql.password());
        assertThat(dataSourceConfigService.getDataSourceConfig(id).getPassword()).isEqualTo(LocalMySql.password());
    }

    @Test
    void createDataSourceConfig_连不上报错() {
        DataSourceConfigSaveReqVO wrongPassword = save(null, "ut-ds");
        wrongPassword.setPassword(LocalMySql.password() + "-wrong");
        assertServiceException(() -> dataSourceConfigService.createDataSourceConfig(wrongPassword),
                DATA_SOURCE_CONFIG_NOT_OK);

        DataSourceConfigSaveReqVO postgres = save(null, "ut-ds");
        postgres.setUrl("jdbc:postgresql://127.0.0.1:5432/ccloud");
        assertServiceException(() -> dataSourceConfigService.createDataSourceConfig(postgres),
                DATA_SOURCE_CONFIG_NOT_OK);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM infra_data_source_config WHERE name = 'ut-ds'", Long.class)).isZero();
    }

    @Test
    void updateDataSourceConfig_提交掩码时沿用原密码() {
        Long id = dataSourceConfigService.createDataSourceConfig(save(null, "ut-ds"));

        DataSourceConfigSaveReqVO req = save(id, "ut-ds-renamed");
        req.setPassword("******");
        dataSourceConfigService.updateDataSourceConfig(req);

        DataSourceConfigDO db = dataSourceConfigService.getDataSourceConfig(id);
        assertThat(db.getName()).isEqualTo("ut-ds-renamed");
        assertThat(db.getPassword()).isEqualTo(LocalMySql.password());
    }

    @Test
    void 主数据源不能修改或删除() {
        assertServiceException(() -> dataSourceConfigService.updateDataSourceConfig(save(0L, "x")),
                DATA_SOURCE_CONFIG_MASTER_READONLY);
        assertServiceException(() -> dataSourceConfigService.deleteDataSourceConfig(0L),
                DATA_SOURCE_CONFIG_MASTER_READONLY);
    }

    @Test
    void deleteDataSourceConfig_已有代码生成表引用时拒绝() {
        Long id = dataSourceConfigService.createDataSourceConfig(save(null, "ut-ds"));
        codegenTableMapper.insert(new CodegenTableDO().setDataSourceConfigId(id).setScene(1).setTableName("ut_table")
                .setTableComment("ut").setModuleName("ut").setBusinessName("table").setClassName("UtTable")
                .setClassComment("ut").setAuthor("ut").setTemplateType(1).setFrontType(10));

        assertServiceException(() -> dataSourceConfigService.deleteDataSourceConfig(id), DATA_SOURCE_CONFIG_IN_USE);

        codegenTableMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CodegenTableDO>()
                .eq("data_source_config_id", id));
        dataSourceConfigService.deleteDataSourceConfig(id);
        assertThat(dataSourceConfigService.getDataSourceConfig(id)).isNull();
    }

    @Test
    void deleteDataSourceConfig_不存在报错() {
        assertServiceException(() -> dataSourceConfigService.deleteDataSourceConfig(-1L), DATA_SOURCE_CONFIG_NOT_EXISTS);
    }

    private static DataSourceConfigSaveReqVO save(Long id, String name) {
        DataSourceConfigSaveReqVO vo = new DataSourceConfigSaveReqVO();
        vo.setId(id);
        vo.setName(name);
        vo.setUrl(LocalMySql.jdbcUrl());
        vo.setUsername(LocalMySql.username());
        vo.setPassword(LocalMySql.password());
        return vo;
    }
}

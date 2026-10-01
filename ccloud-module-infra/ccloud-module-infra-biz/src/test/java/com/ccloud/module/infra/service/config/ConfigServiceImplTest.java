package com.ccloud.module.infra.service.config;

import com.ccloud.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import com.ccloud.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.config.ConfigDO;
import com.ccloud.module.infra.dal.mysql.config.ConfigMapper;
import com.ccloud.module.infra.enums.config.ConfigTypeEnum;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(ConfigServiceImpl.class)
class ConfigServiceImplTest extends BaseInfraDbUnitTest {

    @Resource
    private ConfigServiceImpl configService;
    @Resource
    private ConfigMapper configMapper;

    @Test
    void createConfig_新建为自定义类型_键名重复报错() {
        Long id = configService.createConfig(save(null, "ut.a", "1"));
        ConfigDO db = configMapper.selectById(id);
        assertThat(db.getType()).isEqualTo(ConfigTypeEnum.CUSTOM.getType());
        assertThat(db.getValue()).isEqualTo("1");
        assertServiceException(() -> configService.createConfig(save(null, "ut.a", "2")), CONFIG_KEY_DUPLICATE);
    }

    @Test
    void updateConfig_自定义参数可改键名_内置参数不能改键名但能改值() {
        Long custom = configService.createConfig(save(null, "ut.b", "1"));
        configService.updateConfig(save(custom, "ut.b2", "2"));
        assertThat(configMapper.selectById(custom).getConfigKey()).isEqualTo("ut.b2");

        Long system = insertSystemConfig("ut.sys");
        assertServiceException(() -> configService.updateConfig(save(system, "ut.sys2", "x")),
                CONFIG_KEY_CAN_NOT_UPDATE_SYSTEM_TYPE);
        configService.updateConfig(save(system, "ut.sys", "x"));
        assertThat(configMapper.selectById(system).getValue()).isEqualTo("x");
        assertThat(configMapper.selectById(system).getType()).isEqualTo(ConfigTypeEnum.SYSTEM.getType());
    }

    @Test
    void updateConfig_不存在报错_改成他人键名报错() {
        Long a = configService.createConfig(save(null, "ut.c", "1"));
        configService.createConfig(save(null, "ut.d", "1"));
        assertServiceException(() -> configService.updateConfig(save(a, "ut.d", "1")), CONFIG_KEY_DUPLICATE);
        assertServiceException(() -> configService.updateConfig(save(-1L, "ut.e", "1")), CONFIG_NOT_EXISTS);
    }

    @Test
    void deleteConfig_内置参数不能删除_删除后同一键名可以重建() {
        Long system = insertSystemConfig("ut.sys3");
        assertServiceException(() -> configService.deleteConfig(system), CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        Long custom = configService.createConfig(save(null, "ut.f", "1"));
        configService.deleteConfig(custom);
        assertThat(configService.getConfigByKey("ut.f")).isNull();
        configService.createConfig(save(null, "ut.f", "2"));
        assertThat(configService.getConfigByKey("ut.f").getValue()).isEqualTo("2");
    }

    @Test
    void getConfigPage_按名称与键名模糊查询() {
        configService.createConfig(save(null, "ut.page.x", "1"));
        configService.createConfig(save(null, "ut.other", "1"));
        ConfigPageReqVO reqVO = new ConfigPageReqVO();
        reqVO.setConfigKey("ut.page");
        assertThat(configService.getConfigPage(reqVO).getList()).extracting(ConfigDO::getConfigKey)
                .containsExactly("ut.page.x");
    }

    private Long insertSystemConfig(String key) {
        ConfigDO config = new ConfigDO().setCategory("ut").setType(ConfigTypeEnum.SYSTEM.getType()).setName("内置")
                .setConfigKey(key).setValue("v").setVisible(true);
        configMapper.insert(config);
        return config.getId();
    }

    private static ConfigSaveReqVO save(Long id, String key, String value) {
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setCategory("ut");
        reqVO.setName("单测参数");
        reqVO.setKey(key);
        reqVO.setValue(value);
        reqVO.setVisible(true);
        return reqVO;
    }
}

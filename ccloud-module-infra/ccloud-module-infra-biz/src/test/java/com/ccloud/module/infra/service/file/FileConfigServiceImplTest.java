package com.ccloud.module.infra.service.file;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.ccloud.module.infra.controller.admin.file.vo.FileConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;
import com.ccloud.module.infra.dal.mysql.file.FileConfigMapper;
import com.ccloud.module.infra.framework.file.core.FileClientFactory;
import com.ccloud.module.infra.framework.file.core.FileConfigRefresher;
import com.ccloud.module.infra.framework.file.core.enums.FileStorageEnum;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({FileConfigServiceImpl.class, FileClientFactory.class, LocalValidatorFactoryBean.class})
class FileConfigServiceImplTest extends BaseInfraDbUnitTest {

    @Resource
    private FileConfigServiceImpl fileConfigService;
    @Resource
    private FileConfigMapper fileConfigMapper;
    @Resource
    private FileClientFactory fileClientFactory;
    @MockitoBean
    private FileConfigRefresher fileConfigRefresher;

    @TempDir
    Path dir;

    /**
     * 测试库只有建表脚本，没有种子数据：已有主配置时沿用，否则在本测试自己的事务里补一条（随回滚消失）
     */
    private Long masterId;

    @BeforeEach
    void insertMaster() {
        FileConfigDO master = fileConfigMapper.selectMaster();
        if (master == null) {
            master = new FileConfigDO().setName("单测主配置").setStorage(1).setMaster(true)
                    .setConfig("{\"domain\":\"http://127.0.0.1:28080\"}");
            fileConfigMapper.insert(master);
        }
        masterId = master.getId();
    }

    @Test
    void createFileConfig_按存储器校验配置() {
        assertServiceException(() -> fileConfigService.createFileConfig(save(null, 1, Map.of())), FILE_CONFIG_INVALID);
        assertServiceException(() -> fileConfigService.createFileConfig(
                save(null, 10, Map.of("domain", "http://127.0.0.1:28080"))), FILE_CONFIG_INVALID);
        assertServiceException(() -> fileConfigService.createFileConfig(
                save(null, 11, Map.of("domain", "http://127.0.0.1:28080"))), FILE_CONFIG_INVALID);

        Long id = fileConfigService.createFileConfig(save(null, 10, local()));
        FileConfigDO db = fileConfigMapper.selectById(id);
        assertThat(db.getMaster()).isFalse();
        assertThat(db.getStorage()).isEqualTo(10);
    }

    @Test
    void updateFileConfigMaster_切换后全局只有一个主配置() {
        Long a = fileConfigService.createFileConfig(save(null, 10, local()));
        fileConfigService.updateFileConfigMaster(a);
        assertThat(fileConfigMapper.selectCount(FileConfigDO::getMaster, true)).isEqualTo(1L);
        assertThat(fileConfigMapper.selectMaster().getId()).isEqualTo(a);
        fileConfigService.updateFileConfigMaster(masterId);
        assertThat(fileConfigMapper.selectCount(FileConfigDO::getMaster, true)).isEqualTo(1L);
        assertThat(fileConfigMapper.selectMaster().getId()).isEqualTo(masterId);
    }

    @Test
    void deleteFileConfig_主配置不能删除() {
        assertServiceException(() -> fileConfigService.deleteFileConfig(masterId), FILE_CONFIG_DELETE_FAIL_MASTER);
        Long a = fileConfigService.createFileConfig(save(null, 10, local()));
        fileConfigService.deleteFileConfig(a);
        assertThat(fileConfigMapper.selectById(a)).isNull();
    }

    @Test
    void testFileConfig_返回可读取的地址() throws Exception {
        Long id = fileConfigService.createFileConfig(save(null, 10, local()));
        String url = fileConfigService.testFileConfig(id);
        String prefix = "http://127.0.0.1:28080/admin-api/infra/file/" + id + "/get/test/";
        assertThat(url).startsWith(prefix);
        String path = url.substring(("http://127.0.0.1:28080/admin-api/infra/file/" + id + "/get/").length());
        assertThat(fileClientFactory.getFileClient(id).getContent(path)).isNotNull();
    }

    @Test
    void 配置变更后通知各节点刷新() {
        Long a = fileConfigService.createFileConfig(save(null, 10, local()));
        fileConfigService.updateFileConfig(save(a, 10, local()));
        fileConfigService.updateFileConfigMaster(masterId);
        fileConfigService.deleteFileConfig(a);
        verify(fileConfigRefresher, times(4)).publish();
    }

    @Test
    void S3配置_返回时密钥掩码_以掩码提交修改时沿用原密钥() {
        Long id = fileConfigService.createFileConfig(save(null, 20, s3("real-secret")));
        FileConfigDO db = fileConfigMapper.selectById(id);
        assertThat(db.getStorage()).isEqualTo(20);
        assertThat(secretOf(db)).isEqualTo("real-secret");

        // 返回给前端时按存储器声明的密钥项掩码（与 Controller 的 toRespVO 同一规则）
        Map<String, Object> shown = JsonUtils.parseObject(db.getConfig(), new TypeReference<Map<String, Object>>() {});
        FileStorageEnum.S3.getSecretKeys().forEach(key -> shown.computeIfPresent(key, (k, v) -> DesensitizeUtils.MASK));
        assertThat(shown.get("accessSecret")).isEqualTo("******");
        assertThat(shown.get("accessKey")).isEqualTo("ut-ak");

        // 前端把掩码原样提交：改名称，密钥沿用
        FileConfigSaveReqVO renamed = save(id, 20, shown);
        renamed.setName("改名后");
        fileConfigService.updateFileConfig(renamed);
        db = fileConfigMapper.selectById(id);
        assertThat(db.getName()).isEqualTo("改名后");
        assertThat(secretOf(db)).isEqualTo("real-secret");

        // 提交新密钥：以新值为准
        fileConfigService.updateFileConfig(save(id, 20, s3("new-secret")));
        assertThat(secretOf(fileConfigMapper.selectById(id))).isEqualTo("new-secret");
    }

    @Test
    void S3配置_缺存储桶或密钥报错() {
        Map<String, Object> noBucket = new HashMap<>(s3("real-secret"));
        noBucket.remove("bucket");
        assertServiceException(() -> fileConfigService.createFileConfig(save(null, 20, noBucket)), FILE_CONFIG_INVALID);
        Map<String, Object> noSecret = new HashMap<>(s3("real-secret"));
        noSecret.remove("accessSecret");
        assertServiceException(() -> fileConfigService.createFileConfig(save(null, 20, noSecret)), FILE_CONFIG_INVALID);
    }

    private static Map<String, Object> s3(String secret) {
        return Map.of("endpoint", "https://oss-cn-hangzhou.aliyuncs.com", "bucket", "ccloud-test",
                "accessKey", "ut-ak", "accessSecret", secret, "region", "cn-hangzhou",
                "domain", "https://ccloud-test.oss-cn-hangzhou.aliyuncs.com");
    }

    private static String secretOf(FileConfigDO db) {
        return (String) JsonUtils.parseObject(db.getConfig(), new TypeReference<Map<String, Object>>() {}).get("accessSecret");
    }

    private Map<String, Object> local() {
        return Map.of("basePath", dir.toString(), "domain", "http://127.0.0.1:28080");
    }

    private static FileConfigSaveReqVO save(Long id, Integer storage, Map<String, Object> config) {
        FileConfigSaveReqVO reqVO = new FileConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setName("单测文件配置");
        reqVO.setStorage(storage);
        reqVO.setConfig(config);
        return reqVO;
    }
}

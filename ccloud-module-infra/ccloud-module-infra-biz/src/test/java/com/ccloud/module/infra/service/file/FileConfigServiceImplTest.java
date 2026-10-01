package com.ccloud.module.infra.service.file;

import com.ccloud.module.infra.controller.admin.file.vo.FileConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;
import com.ccloud.module.infra.dal.mysql.file.FileConfigMapper;
import com.ccloud.module.infra.framework.file.core.FileClientFactory;
import com.ccloud.module.infra.framework.file.core.FileConfigRefresher;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.file.Path;
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
     * 测试库只有建表脚本，没有种子数据；Web 测试会把种子主配置提交进库，因此已有主配置时直接沿用，否则补一条，
     * 对应生产种子中的编号 1
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

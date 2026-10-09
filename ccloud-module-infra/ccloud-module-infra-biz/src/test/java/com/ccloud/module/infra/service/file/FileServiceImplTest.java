package com.ccloud.module.infra.service.file;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.infra.dal.dataobject.file.FileConfigDO;
import com.ccloud.module.infra.dal.dataobject.file.FileContentDO;
import com.ccloud.module.infra.dal.dataobject.file.FileDO;
import com.ccloud.module.infra.dal.mysql.file.FileConfigMapper;
import com.ccloud.module.infra.dal.mysql.file.FileContentMapper;
import com.ccloud.module.infra.dal.mysql.file.FileMapper;
import com.ccloud.module.infra.framework.file.config.FileProperties;
import com.ccloud.module.infra.framework.file.core.FileClientFactory;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;
import static com.ccloud.module.infra.framework.file.core.utils.FileTypeUtilsTest.PNG;
import static org.assertj.core.api.Assertions.assertThat;

@Import({FileServiceImpl.class, FileClientFactory.class, FileServiceImplTest.PropertiesConfig.class})
class FileServiceImplTest extends BaseInfraDbUnitTest {

    @TestConfiguration
    @EnableConfigurationProperties(FileProperties.class)
    static class PropertiesConfig {
    }

    @Resource
    private FileServiceImpl fileService;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileContentMapper fileContentMapper;
    @Resource
    private FileConfigMapper fileConfigMapper;

    /**
     * 测试库只有建表脚本，没有种子数据：已有主配置时沿用，否则在本测试自己的事务里补一条（随回滚消失），
     * 不依赖其他测试类；断言一律对照实际使用的主配置编号
     */
    private Long masterId;

    @BeforeEach
    void ensureMaster() {
        FileConfigDO master = fileConfigMapper.selectMaster();
        if (master == null) {
            master = new FileConfigDO().setName("单测主配置").setStorage(1).setMaster(true)
                    .setConfig("{\"domain\":\"http://127.0.0.1:28080\"}");
            fileConfigMapper.insert(master);
        }
        masterId = master.getId();
    }

    @Test
    void createFile_按内容哈希存储_地址指向读取接口() {
        String url = fileService.createFile("头像.png", PNG);

        assertThat(url).matches("http://127\\.0\\.0\\.1:28080/admin-api/infra/file/" + masterId
                + "/get/\\d{8}/[0-9a-f]{64}\\.png");
        List<FileDO> files = fileMapper.selectList();
        assertThat(files).hasSize(1);
        FileDO file = files.get(0);
        assertThat(file.getConfigId()).isEqualTo(masterId);
        assertThat(file.getName()).isEqualTo("头像.png");
        assertThat(file.getType()).isEqualTo("image/png");
        assertThat(file.getSize()).isEqualTo(PNG.length);
        assertThat(file.getUrl()).isEqualTo(url);
        assertThat(url).endsWith("/get/" + file.getPath());
        assertThat(fileService.getFileContent(masterId, file.getPath())).isEqualTo(PNG);
    }

    @Test
    void createFile_相同内容只存一份_每次上传各记一条() {
        fileService.createFile("a.png", PNG);
        fileService.createFile("b.png", PNG);

        List<FileDO> files = fileMapper.selectList();
        assertThat(files).hasSize(2);
        assertThat(files.get(0).getPath()).isEqualTo(files.get(1).getPath());
        assertThat(files).extracting(FileDO::getName).containsExactlyInAnyOrder("a.png", "b.png");
        assertThat(fileContentMapper.selectCount(FileContentDO::getPath, files.get(0).getPath())).isEqualTo(1L);
    }

    @Test
    void createFile_类型不允许_超过大小_空内容() {
        byte[] html = "<!DOCTYPE html><html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        assertServiceException(() -> fileService.createFile("a.png", html), FILE_TYPE_NOT_ALLOWED);
        assertServiceException(() -> fileService.createFile("a.bin", new byte[10 * 1024 * 1024 + 1]), FILE_SIZE_EXCEEDED);
        assertServiceException(() -> fileService.createFile("a.png", new byte[0]), FILE_EMPTY);
        assertServiceException(() -> fileService.createFile("a.png", null), FILE_EMPTY);
        assertThat(fileMapper.selectCount()).isZero();
    }

    @Test
    void deleteFile_仍有引用时保留内容_最后一条删除时删内容() {
        fileService.createFile("a.png", PNG);
        fileService.createFile("b.png", PNG);
        List<FileDO> files = fileMapper.selectList();
        String path = files.get(0).getPath();

        fileService.deleteFile(files.get(0).getId());
        assertThat(fileService.getFileContent(masterId, path)).isEqualTo(PNG);

        fileService.deleteFile(files.get(1).getId());
        assertThat(fileService.getFileContent(masterId, path)).isNull();

        assertServiceException(() -> fileService.deleteFile(files.get(1).getId()), FILE_NOT_EXISTS);
        assertServiceException(() -> fileService.deleteFile(-1L), FILE_NOT_EXISTS);
    }

    @Test
    void deleteFile_其他租户的引用也算() {
        fileService.createFile("t1.png", PNG);
        TenantUtils.execute(2L, () -> fileService.createFile("t2.png", PNG));
        FileDO mine = fileMapper.selectList().get(0);
        assertThat(mine.getName()).isEqualTo("t1.png");

        fileService.deleteFile(mine.getId());

        assertThat(fileService.getFileContent(masterId, mine.getPath())).isEqualTo(PNG);
    }

    @Test
    void getFileContent_读取不存在的路径返回null() {
        assertThat(fileService.getFileContent(masterId, "20200101/" + "0".repeat(64) + ".png")).isNull();
    }
}

package com.ccloud.module.infra.framework.file.core.client;

import com.ccloud.module.infra.dal.dataobject.file.FileContentDO;
import com.ccloud.module.infra.dal.mysql.file.FileContentMapper;
import com.ccloud.module.infra.framework.file.core.client.db.DBFileClient;
import com.ccloud.module.infra.framework.file.core.client.db.DBFileClientConfig;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DBFileClientTest extends BaseInfraDbUnitTest {

    @Resource
    private FileContentMapper fileContentMapper;

    private DBFileClient client(long id) {
        DBFileClientConfig config = new DBFileClientConfig();
        config.setDomain("http://127.0.0.1:28080");
        return new DBFileClient(id, config, fileContentMapper);
    }

    @Test
    void 上传读取删除_地址指向读取接口() throws Exception {
        DBFileClient client = client(8L);
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        String url = client.upload(content, "20260930/abc.txt", "text/plain");
        assertThat(url).isEqualTo("http://127.0.0.1:28080/admin-api/infra/file/8/get/20260930/abc.txt");
        assertThat(client.getContent("20260930/abc.txt")).isEqualTo(content);
        client.delete("20260930/abc.txt");
        assertThat(client.getContent("20260930/abc.txt")).isNull();
    }

    @Test
    void 同一路径重复上传只插入一行() throws Exception {
        DBFileClient client = client(8L);
        client.upload("first".getBytes(StandardCharsets.UTF_8), "d/x.txt", "text/plain");
        client.upload("second".getBytes(StandardCharsets.UTF_8), "d/x.txt", "text/plain");
        assertThat(fileContentMapper.selectCount(FileContentDO::getPath, "d/x.txt")).isEqualTo(1L);
        assertThat(new String(client.getContent("d/x.txt"), StandardCharsets.UTF_8)).isEqualTo("first");
    }

    @Test
    void 不同配置编号的同名路径互不影响() throws Exception {
        DBFileClient a = client(8L);
        DBFileClient b = client(9L);
        a.upload("a".getBytes(StandardCharsets.UTF_8), "d/x.txt", "text/plain");
        b.upload("b".getBytes(StandardCharsets.UTF_8), "d/x.txt", "text/plain");
        assertThat(new String(a.getContent("d/x.txt"), StandardCharsets.UTF_8)).isEqualTo("a");
        assertThat(new String(b.getContent("d/x.txt"), StandardCharsets.UTF_8)).isEqualTo("b");
        a.delete("d/x.txt");
        assertThat(a.getContent("d/x.txt")).isNull();
        assertThat(new String(b.getContent("d/x.txt"), StandardCharsets.UTF_8)).isEqualTo("b");
    }
}

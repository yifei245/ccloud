package com.ccloud.module.infra.framework.file.core.client;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.infra.framework.file.core.client.local.LocalFileClient;
import com.ccloud.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileClientTest {

    @TempDir
    Path base;

    private LocalFileClient client() {
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setBasePath(base.resolve("root").toString());
        config.setDomain("http://127.0.0.1:28080");
        return new LocalFileClient(7L, config);
    }

    @Test
    void 上传读取删除_地址指向读取接口() throws Exception {
        LocalFileClient client = client();
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        String url = client.upload(content, "20260930/abc.txt", "text/plain");
        assertThat(url).isEqualTo("http://127.0.0.1:28080/admin-api/infra/file/7/get/20260930/abc.txt");
        assertThat(client.getContent("20260930/abc.txt")).isEqualTo(content);
        client.delete("20260930/abc.txt");
        assertThat(client.getContent("20260930/abc.txt")).isNull();
    }

    @Test
    void 同一路径重复上传不覆盖() throws Exception {
        LocalFileClient client = client();
        client.upload("first".getBytes(StandardCharsets.UTF_8), "d/x.txt", "text/plain");
        client.upload("second".getBytes(StandardCharsets.UTF_8), "d/x.txt", "text/plain");
        assertThat(new String(client.getContent("d/x.txt"), StandardCharsets.UTF_8)).isEqualTo("first");
    }

    @Test
    void 越出根目录的路径一律拒绝() throws Exception {
        LocalFileClient client = client();
        Files.writeString(base.resolve("outside.txt"), "secret");
        for (String path : new String[]{"../outside.txt", "d/../../outside.txt", "/etc/passwd", "d/./../../outside.txt", "a\0b.png"}) {
            assertThatThrownBy(() -> client.getContent(path)).as(path).isInstanceOf(ServiceException.class);
            assertThatThrownBy(() -> client.upload(new byte[]{1}, path, "text/plain")).as(path).isInstanceOf(ServiceException.class);
            assertThatThrownBy(() -> client.delete(path)).as(path).isInstanceOf(ServiceException.class);
        }
        assertThat(Files.readString(base.resolve("outside.txt"))).isEqualTo("secret");
    }
}

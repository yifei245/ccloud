package com.ccloud.module.infra.framework.file.core.client;

import com.ccloud.module.infra.framework.file.core.client.s3.S3FileClient;
import com.ccloud.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import com.ccloud.module.infra.framework.file.core.utils.FileTypeUtilsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class S3FileClientTest {

    private static final String BUCKET = "ccloud-test";
    private static FakeS3Server server;
    private static S3FileClient client;

    @BeforeAll
    static void setUp() {
        server = new FakeS3Server();
        server.start();
        S3FileClientConfig config = new S3FileClientConfig();
        config.setEndpoint(server.endpoint());
        config.setBucket(BUCKET);
        config.setAccessKey("ut-ak");
        config.setAccessSecret("ut-sk");
        config.setRegion("cn-hangzhou");
        config.setPathStyle(true);
        config.setDomain("https://cdn.ccloud.local/");
        client = new S3FileClient(9L, config);
    }

    @AfterAll
    static void tearDown() {
        server.stop();
    }

    @BeforeEach
    void clear() {
        server.clear();
    }

    @Test
    void 上传_按路径风格写入存储桶_带签名与类型_地址为访问域名加路径() throws Exception {
        String url = client.upload(FileTypeUtilsTest.PNG, "20261001/abc.png", "image/png");
        assertThat(url).isEqualTo("https://cdn.ccloud.local/20261001/abc.png");
        FakeS3Server.RecordedRequest put = server.requests().get(0);
        assertThat(put.method()).isEqualTo("PUT");
        assertThat(put.path()).isEqualTo("/ccloud-test/20261001/abc.png");
        assertThat(put.header("Content-Type")).isEqualTo("image/png");
        assertThat(put.header("Authorization")).startsWith("AWS4-HMAC-SHA256 Credential=ut-ak/").contains("/cn-hangzhou/s3/");
        assertThat(put.body()).isEqualTo(FileTypeUtilsTest.PNG);
    }

    @Test
    void 上传_不附加新版SDK默认的CRC校验头_兼容阿里云OSS等存储() throws Exception {
        client.upload(FileTypeUtilsTest.PNG, "20261001/crc.png", "image/png");
        FakeS3Server.RecordedRequest put = server.requests().get(0);
        assertThat(put.headerNames()).noneMatch(name -> name.toLowerCase(Locale.ROOT).startsWith("x-amz-checksum-"));
        assertThat(put.header("x-amz-sdk-checksum-algorithm")).isNull();
    }

    @Test
    void 读取已上传的内容_删除后读取为空() throws Exception {
        client.upload(FileTypeUtilsTest.PNG, "20261001/r.png", "image/png");
        assertThat(client.getContent("20261001/r.png")).isEqualTo(FileTypeUtilsTest.PNG);
        client.delete("20261001/r.png");
        assertThat(client.getContent("20261001/r.png")).isNull();
    }

    @Test
    void 读取不存在的对象返回null() throws Exception {
        assertThat(client.getContent("20261001/none.png")).isNull();
    }
}

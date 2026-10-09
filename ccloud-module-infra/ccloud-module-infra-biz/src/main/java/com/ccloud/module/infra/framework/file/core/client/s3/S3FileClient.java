package com.ccloud.module.infra.framework.file.core.client.s3;

import cn.hutool.core.util.StrUtil;
import com.ccloud.module.infra.framework.file.core.client.AbstractFileClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/**
 * S3 兼容存储（AWS S3、阿里云 OSS 等）。文件由存储桶或其前置 CDN 直接公开读取
 */
public class S3FileClient extends AbstractFileClient<S3FileClientConfig> {

    private final S3Client s3;

    public S3FileClient(Long id, S3FileClientConfig config) {
        super(id, config);
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .region(Region.of(StrUtil.blankToDefault(config.getRegion(), "us-east-1")))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getAccessSecret())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(Boolean.TRUE.equals(config.getPathStyle())).build())
                // 新版 SDK 默认附加 CRC 校验头，阿里云 OSS 等 S3 兼容存储不认，只在服务端要求时计算
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        s3.putObject(PutObjectRequest.builder().bucket(config.getBucket()).key(path).contentType(type).build(),
                RequestBody.fromBytes(content));
        return StrUtil.removeSuffix(config.getDomain(), "/") + "/" + path;
    }

    @Override
    public void delete(String path) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(config.getBucket()).key(path).build());
    }

    @Override
    public byte[] getContent(String path) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(config.getBucket()).key(path).build()).asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }
}

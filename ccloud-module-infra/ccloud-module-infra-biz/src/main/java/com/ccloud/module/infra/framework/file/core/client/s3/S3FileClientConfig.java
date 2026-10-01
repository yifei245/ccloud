package com.ccloud.module.infra.framework.file.core.client.s3;

import com.ccloud.module.infra.framework.file.core.client.FileClientConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

/**
 * S3 兼容存储配置，适用于 AWS S3 与阿里云 OSS（OSS 兼容 S3 协议），具体填写以各云厂商的 S3 兼容文档为准。
 * <p>
 * AWS S3：endpoint 如 https://s3.ap-east-1.amazonaws.com，region 填 ap-east-1，domain 如 https://{bucket}.s3.ap-east-1.amazonaws.com。
 * 阿里云 OSS：endpoint 如 https://oss-cn-hangzhou.aliyuncs.com，region 填 cn-hangzhou，domain 如 https://{bucket}.oss-cn-hangzhou.aliyuncs.com。
 * 两者都使用虚拟主机风格，pathStyle 保持默认。domain 也可以填存储桶前置的 CDN 域名。
 * <p>
 * 文件由存储桶（或其前置 CDN）直接公开读取，infra 的读取接口不经手；存储桶的公开读权限由使用者在云控制台配置。
 */
@Data
public class S3FileClientConfig implements FileClientConfig {

    /**
     * 接入地址
     */
    @NotBlank(message = "接入地址不能为空")
    @URL(message = "接入地址须为 http(s) 地址")
    private String endpoint;

    @NotBlank(message = "存储桶不能为空")
    private String bucket;

    @NotBlank(message = "访问密钥编号不能为空")
    private String accessKey;

    @NotBlank(message = "访问密钥不能为空")
    private String accessSecret;

    /**
     * 地域，可空，默认 us-east-1；阿里云 OSS 填地域编号，如 cn-hangzhou
     */
    private String region;

    /**
     * 是否路径风格访问，可空，默认 false（虚拟主机风格）
     */
    private Boolean pathStyle;

    /**
     * 访问域名，文件地址为该域名加文件路径
     */
    @NotBlank(message = "访问域名不能为空")
    @URL(message = "访问域名须为 http(s) 地址")
    private String domain;
}

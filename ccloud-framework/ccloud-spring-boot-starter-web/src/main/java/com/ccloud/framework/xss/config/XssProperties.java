package com.ccloud.framework.xss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 过滤配置
 */
@ConfigurationProperties(prefix = "ccloud.xss")
@Data
public class XssProperties {

    private boolean enable = true;

    /**
     * 排除路径。富文本编辑器提交的内容若被清洗会破坏格式，可在此排除。
     */
    private List<String> excludeUrls = new ArrayList<>();
}

package com.ccloud.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档链接。异常提示里附带排查文档地址，减少沟通成本。
 */
@Getter
@AllArgsConstructor
public enum DocumentEnum {

    TENANT("https://github.com/yifei/ccloud/blob/main/docs/模块说明.md#多租户", "多租户"),
    INTERFACE_AUTH("https://github.com/yifei/ccloud/blob/main/docs/接口鉴权说明.md", "接口鉴权"),
    QUICK_START("https://github.com/yifei/ccloud/blob/main/docs/快速开始.md", "快速开始");

    private final String url;
    private final String name;
}

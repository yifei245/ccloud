package com.ccloud.module.infra.enums.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 代码生成的场景
 */
@Getter
@AllArgsConstructor
public enum CodegenSceneEnum {

    ADMIN(1, "admin", "管理后台", "/admin-api", ""),
    APP(2, "app", "用户 App", "/app-api", "App");

    /**
     * 场景
     */
    private final Integer scene;
    /**
     * controller 下的子包名
     */
    private final String basePackage;
    /**
     * 名称
     */
    private final String label;
    /**
     * 接口地址前缀
     */
    private final String urlPrefix;
    /**
     * 类名前缀
     */
    private final String prefix;

    /**
     * @return 场景不存在返回 null
     */
    public static CodegenSceneEnum valueOf(Integer scene) {
        return Arrays.stream(values()).filter(e -> e.getScene().equals(scene)).findFirst().orElse(null);
    }
}

package com.ccloud.module.system.enums.oauth2;

import java.util.List;

/**
 * 内置 OAuth2 客户端编号，与 sql/mysql/dml/05-oauth2-client.sql 一致
 */
public interface OAuth2ClientConstants {

    String CLIENT_ID_WEB = "ccloud-web";
    String CLIENT_ID_APP = "ccloud-app";
    String CLIENT_ID_MP_WEIXIN = "ccloud-mp-weixin";
    String CLIENT_ID_H5 = "ccloud-h5";
    String CLIENT_ID_OPEN_DEMO = "ccloud-open-demo";

    /**
     * 请求未显式携带客户端编号时，管理端默认使用 web，用户端默认使用 app
     */
    String CLIENT_ID_DEFAULT_ADMIN = CLIENT_ID_WEB;
    String CLIENT_ID_DEFAULT_MEMBER = CLIENT_ID_APP;

    /**
     * 请求头：用于指定客户端编号，区分同一用户在不同终端的登录
     */
    String HEADER_CLIENT_ID = "X-Client-Id";

    List<String> ALL_CLIENT_IDS = List.of(CLIENT_ID_WEB, CLIENT_ID_APP, CLIENT_ID_MP_WEIXIN, CLIENT_ID_H5,
            CLIENT_ID_OPEN_DEMO);
}

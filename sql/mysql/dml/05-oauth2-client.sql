-- ----------------------------------------------------------------------------
-- 初始化数据：OAuth2 客户端
-- 每个终端一个客户端，独立配置令牌时长、授权类型与 scope。密钥为本地开发默认值，生产环境必须在后台重置
-- ----------------------------------------------------------------------------
USE `ccloud`;

INSERT INTO `system_oauth2_client` (`id`, `client_id`, `secret`, `name`, `logo`, `description`, `status`, `access_token_validity_seconds`, `refresh_token_validity_seconds`, `redirect_uris`, `authorized_grant_types`, `scopes`, `auto_approve_scopes`, `authorities`, `resource_ids`, `additional_information`, `creator`) VALUES
  (1, 'ccloud-web', 'ccloud-web-secret', '管理后台', '', 'Web 管理后台，支持 SSO 授权码模式', 0, 1800, 2592000, '["http://localhost:8080/cb","http://127.0.0.1:8080/cb"]', '["password","sms_code","social","authorization_code","refresh_token"]', '["user.read","user.write"]', '["user.read"]', '[]', '[]', '{}', '1'),
  (2, 'ccloud-app', 'ccloud-app-secret', '手机 App', '', 'iOS / Android App', 0, 7200, 2592000, '[]', '["password","sms_code","social","refresh_token"]', '["user.read","user.write"]', '[]', '[]', '[]', '{}', '1'),
  (3, 'ccloud-mp-weixin', 'ccloud-mp-weixin-secret', '微信小程序', '', '微信小程序，支持一键登录', 0, 7200, 2592000, '[]', '["sms_code","social","refresh_token"]', '["user.read","user.write"]', '[]', '[]', '[]', '{}', '1'),
  (4, 'ccloud-h5', 'ccloud-h5-secret', 'H5 网页', '', '移动端 H5 与微信公众号 H5', 0, 7200, 2592000, '[]', '["password","sms_code","social","refresh_token"]', '["user.read","user.write"]', '[]', '[]', '[]', '{}', '1'),
  (5, 'ccloud-open-demo', 'ccloud-open-demo-secret', '开放平台演示应用', '', '第三方系统以 client_credentials 换取令牌调用 /open-api', 0, 7200, 0, '[]', '["client_credentials"]', '["member.user.read","trade.order.read"]', '[]', '[]', '[]', '{}', '1');


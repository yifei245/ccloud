-- ----------------------------------------------------------------------------
-- 初始化数据：默认配置与演示数据
-- 短信模拟渠道、文件存储、参数、会员等级、交易配置、开放应用、支付演示应用
-- ----------------------------------------------------------------------------
USE `ccloud`;

INSERT INTO `system_sms_channel` (`id`, `signature`, `code`, `status`, `remark`, `api_key`, `api_secret`, `creator`) VALUES
  (1, 'ccloud', 'MOCK', 0, '本地调试渠道：不真正发送，验证码写入日志与 Redis', 'mock', 'mock', '1');

INSERT INTO `system_sms_template` (`id`, `type`, `status`, `code`, `name`, `content`, `params`, `remark`, `api_template_id`, `channel_id`, `channel_code`, `creator`) VALUES
  (1, 1, 0, 'user-sms-login', '会员-手机号登录', '您的验证码为 {code}，5 分钟内有效', '["code"]', NULL, 'mock', 1, 'MOCK', '1'),
  (2, 1, 0, 'user-update-mobile', '会员-修改手机', '您正在修改手机号，验证码为 {code}', '["code"]', NULL, 'mock', 1, 'MOCK', '1'),
  (3, 1, 0, 'user-update-password', '会员-修改密码', '您正在修改密码，验证码为 {code}', '["code"]', NULL, 'mock', 1, 'MOCK', '1'),
  (4, 1, 0, 'user-reset-password', '会员-忘记密码', '您正在重置密码，验证码为 {code}', '["code"]', NULL, 'mock', 1, 'MOCK', '1'),
  (5, 1, 0, 'admin-sms-login', '后台-手机号登录', '您正在登录管理后台，验证码为 {code}', '["code"]', NULL, 'mock', 1, 'MOCK', '1'),
  (6, 1, 0, 'admin-reset-password', '后台-忘记密码', '您正在重置后台密码，验证码为 {code}', '["code"]', NULL, 'mock', 1, 'MOCK', '1');

INSERT INTO `system_notify_template` (`id`, `name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `creator`) VALUES
  (1, '测试站内信', 'test', '系统', '您好 {name}，这是一条测试站内信', 2, '["name"]', 0, '1');

INSERT INTO `system_notice` (`id`, `title`, `content`, `type`, `status`, `tenant_id`, `creator`) VALUES
  (1, '欢迎使用 ccloud', '<p>ccloud 微服务平台已就绪。</p>', 2, 0, 1, '1');

INSERT INTO `infra_file_config` (`id`, `name`, `storage`, `remark`, `master`, `config`, `creator`) VALUES
  (1, '数据库存储', 1, '默认主配置，开箱即用；生产环境建议切换为 S3 兼容存储', b'1', '{"domain":"http://127.0.0.1:28080"}', '1');

INSERT INTO `infra_config` (`id`, `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`) VALUES
  (1, 'biz', 1, '用户管理-账号初始密码', 'system.user.init-password', '123456', b'0', '导入用户时使用，不返回给前端', '1'),
  (2, 'biz', 1, '管理后台-是否开放注册', 'system.auth.register-enabled', 'false', b'1', 'true 时登录页可自助注册管理员账号', '1');

INSERT INTO `member_level` (`id`, `name`, `level`, `experience`, `discount_percent`, `icon`, `background_url`, `status`, `tenant_id`, `creator`) VALUES
  (1, '青铜会员', 1, 0, 100, '', '', 0, 1, '1'),
  (2, '白银会员', 2, 1000, 98, '', '', 0, 1, '1'),
  (3, '黄金会员', 3, 5000, 95, '', '', 0, 1, '1'),
  (4, '钻石会员', 4, 20000, 90, '', '', 0, 1, '1');

INSERT INTO `member_config` (`id`, `point_trade_deduct_enable`, `point_trade_deduct_unit_price`, `point_trade_deduct_max_price`, `point_trade_give_point`, `tenant_id`, `creator`) VALUES
  (1, b'1', 1, 10000, 1, 1, '1');

INSERT INTO `member_sign_in_config` (`id`, `day`, `point`, `experience`, `status`, `tenant_id`, `creator`) VALUES
  (1, 1, 2, 1, 0, 1, '1'),
  (2, 2, 4, 2, 0, 1, '1'),
  (3, 3, 6, 3, 0, 1, '1'),
  (4, 4, 8, 4, 0, 1, '1'),
  (5, 5, 10, 5, 0, 1, '1'),
  (6, 6, 12, 6, 0, 1, '1'),
  (7, 7, 14, 7, 0, 1, '1');

INSERT INTO `trade_config` (`id`, `after_sale_refund_reasons`, `after_sale_return_reasons`, `delivery_express_free_enabled`, `delivery_express_free_price`, `delivery_pick_up_enabled`, `brokerage_enabled`, `brokerage_enabled_condition`, `brokerage_bind_mode`, `brokerage_first_percent`, `brokerage_second_percent`, `brokerage_withdraw_min_price`, `brokerage_withdraw_fee_percent`, `brokerage_frozen_days`, `brokerage_withdraw_types`, `tenant_id`, `creator`) VALUES
  (1, '["不想要了","商品与描述不符","质量问题"]', '["商品破损","发错货","质量问题"]', b'1', 9900, b'1', b'1', 1, 1, 10, 5, 100, 0, 7, '[1,2,3,4]', 1, '1');

INSERT INTO `trade_delivery_express` (`id`, `code`, `name`, `logo`, `sort`, `status`, `tenant_id`, `creator`) VALUES
  (1, 'SF', '顺丰速运', '', 1, 0, 1, '1'),
  (2, 'YTO', '圆通速递', '', 2, 0, 1, '1'),
  (3, 'ZTO', '中通快递', '', 3, 0, 1, '1'),
  (4, 'JD', '京东快递', '', 4, 0, 1, '1');

INSERT INTO `pay_app` (`id`, `app_key`, `name`, `status`, `remark`, `open_app_id`, `tenant_id`, `creator`) VALUES
  (1, 'mall', '商城', 0, '商城下单支付使用', 1, 1, '1'),
  (2, 'wallet', '钱包充值', 0, '钱包充值使用', NULL, 1, '1');

INSERT INTO `pay_channel` (`id`, `code`, `status`, `remark`, `fee_rate`, `app_id`, `config`, `tenant_id`, `creator`) VALUES
  (1, 'mock', 0, '模拟支付：本地调试用，生产环境不可用', 0, 1, 'd0a6627d2f92ec6989e256b20e6e9a26a6cd6905644a1d75fd3c121d153388cc282f016c3932891ff4f31fd2c976acb18a8dc38afe02b0fd786e01c542bc921a5ed6af55cfc0720d09d46dd704729e796da82a51125021f21936db353814ba61', 1, '1'),
  (2, 'wallet', 0, '钱包余额支付', 0, 1, '649dcb7aad6bc5c4b1dfb98035c7acca', 1, '1'),
  (3, 'mock', 0, '模拟支付：本地调试用，生产环境不可用', 0, 2, 'd0a6627d2f92ec6989e256b20e6e9a26a6cd6905644a1d75fd3c121d153388cc282f016c3932891ff4f31fd2c976acb18a8dc38afe02b0fd786e01c542bc921a5ed6af55cfc0720d09d46dd704729e796da82a51125021f21936db353814ba61', 1, '1');

INSERT INTO `pay_wallet_recharge_package` (`id`, `name`, `pay_price`, `bonus_price`, `status`, `tenant_id`, `creator`) VALUES
  (1, '充 100 送 10', 10000, 1000, 0, 1, '1'),
  (2, '充 500 送 80', 50000, 8000, 0, 1, '1');

INSERT INTO `pay_wallet` (`id`, `user_id`, `user_type`, `balance`, `total_expense`, `total_recharge`, `freeze_price`, `tenant_id`, `creator`) VALUES
  (1, 1, 1, 0, 0, 0, 0, 1, '1');

INSERT INTO `open_app` (`id`, `name`, `app_key`, `app_secret`, `status`, `remark`, `ip_whitelist`, `rate_limit`, `expire_time`, `contact_name`, `contact_mobile`, `notify_url`, `tenant_id`, `creator`) VALUES
  (1, '开放平台演示应用', 'ccloud-open-demo', '762055db4f2bfcd6b747242eafabcdc81c40e79a07ef2482c4d918b301c507c6', 0, '本地调试用，密钥原文 ccloud-open-demo-secret；生产环境必须删除（密文用开发默认加密密钥加密，换密钥后无法解密）', NULL, 50, NULL, '演示联系人', '13800000009', NULL, 1, '1');

INSERT INTO `open_api` (`id`, `name`, `code`, `path`, `method`, `description`, `status`, `creator`) VALUES
  (1, '查询会员信息', 'member.user.get', '/open-api/member/user/get', 'GET', '按手机号查询会员基本信息，手机号脱敏', 0, '1'),
  (2, '创建支付单', 'pay.order.create', '/open-api/pay/order/create', 'POST', '按商户订单号创建支付单，重复创建返回原单', 0, '1'),
  (3, '查询支付单', 'pay.order.get', '/open-api/pay/order/get', 'GET', '按商户订单号查询支付单', 0, '1'),
  (4, '提交支付', 'pay.order.submit', '/open-api/pay/order/submit', 'POST', '选择渠道发起支付，返回二维码、跳转链接等展示信息；不可选钱包', 0, '1'),
  (5, '发起退款', 'pay.refund.create', '/open-api/pay/refund/create', 'POST', '按商户退款单号发起退款，重复发起返回原退款单', 0, '1'),
  (6, '查询退款', 'pay.refund.get', '/open-api/pay/refund/get', 'GET', '按商户退款单号查询退款单', 0, '1');

INSERT INTO `open_app_api` (`id`, `app_id`, `api_id`, `tenant_id`, `creator`) VALUES
  (1, 1, 1, 1, '1'),
  (2, 1, 2, 1, '1'),
  (3, 1, 3, 1, '1'),
  (4, 1, 4, 1, '1'),
  (5, 1, 5, 1, '1'),
  (6, 1, 6, 1, '1');


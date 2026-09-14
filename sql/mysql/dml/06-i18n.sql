-- ----------------------------------------------------------------------------
-- 初始化数据：国际化文案
-- 错误码文案，key 为 error.code.{错误码}。system 模块错误码在 Task 23 定义后追加
-- ----------------------------------------------------------------------------
USE `ccloud`;

INSERT INTO `system_i18n_message` (`id`, `locale`, `code`, `content`, `source`, `creator`) VALUES
  (1, 'zh_CN', 'error.code.400', '请求参数不正确', 1, '1'),
  (2, 'en_US', 'error.code.400', 'Bad request', 1, '1'),
  (3, 'zh_CN', 'error.code.401', '账号未登录', 1, '1'),
  (4, 'en_US', 'error.code.401', 'Unauthorized', 1, '1'),
  (5, 'zh_CN', 'error.code.403', '没有该操作权限', 1, '1'),
  (6, 'en_US', 'error.code.403', 'Forbidden', 1, '1'),
  (7, 'zh_CN', 'error.code.404', '请求未找到', 1, '1'),
  (8, 'en_US', 'error.code.404', 'Not found', 1, '1'),
  (9, 'zh_CN', 'error.code.405', '请求方法不正确', 1, '1'),
  (10, 'en_US', 'error.code.405', 'Method not allowed', 1, '1'),
  (11, 'zh_CN', 'error.code.423', '请求失败，请稍后重试', 1, '1'),
  (12, 'en_US', 'error.code.423', 'Locked, please try again later', 1, '1'),
  (13, 'zh_CN', 'error.code.429', '请求过于频繁，请稍后重试', 1, '1'),
  (14, 'en_US', 'error.code.429', 'Too many requests, please try again later', 1, '1'),
  (15, 'zh_CN', 'error.code.500', '系统异常', 1, '1'),
  (16, 'en_US', 'error.code.500', 'Internal server error', 1, '1'),
  (17, 'zh_CN', 'error.code.501', '功能未实现或未开启', 1, '1'),
  (18, 'en_US', 'error.code.501', 'Not implemented', 1, '1'),
  (19, 'zh_CN', 'error.code.502', '错误的配置项', 1, '1'),
  (20, 'en_US', 'error.code.502', 'Invalid configuration', 1, '1'),
  (21, 'zh_CN', 'error.code.900', '重复请求，请稍后重试', 1, '1'),
  (22, 'en_US', 'error.code.900', 'Repeated request, please try again later', 1, '1'),
  (23, 'zh_CN', 'error.code.901', '演示模式，禁止写操作', 1, '1'),
  (24, 'en_US', 'error.code.901', 'Demo mode, write operations are disabled', 1, '1'),
  (25, 'zh_CN', 'error.code.999', '未知错误', 1, '1'),
  (26, 'en_US', 'error.code.999', 'Unknown error', 1, '1');


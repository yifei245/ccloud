#!/bin/bash
# 登记 ccloud 的 XXL-Job 执行器与任务（可重复执行，已登记的跳过）：
#   - 删除上游示例执行器与任务，登记 system、infra、open、pay 四个执行器及其任务，并把默认管理员密码 123456 替换为 .env 中的密码
# MySQL 容器首次初始化时执行（位于 01 ccloud 库、02 Nacos 库、03 XXL-Job 库之后），scripts/docker-up.sh 每次也会执行
# 任务处理器名须与代码中的 @XxlJob 一致（system 的 MqJobHandler，infra 的 MqJobHandler 与 ApiLogCleanJob，open 的 MqJobHandler、回调重试与日志清理任务，pay 的超时关单、支付与退款与转账补查任务和 MqJobHandler）
set -euo pipefail

: "${XXL_JOB_ADMIN_PASSWORD:?必须设置 XXL_JOB_ADMIN_PASSWORD}"
password_hash=$(printf '%s' "$XXL_JOB_ADMIN_PASSWORD" | sha256sum | cut -d' ' -f1)

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" xxl_job <<SQL
DELETE i FROM xxl_job_info i JOIN xxl_job_group g ON i.job_group = g.id
 WHERE g.app_name LIKE 'xxl-job-executor-sample%';
DELETE FROM xxl_job_group WHERE app_name LIKE 'xxl-job-executor-sample%';

-- address_type=0：执行器启动后自动注册地址。appname 与 xxl.job.executor.appname（即服务名）一致
INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
SELECT 'ccloud-system-server', 'system 服务', 0, NULL, NOW() FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'ccloud-system-server');
INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
SELECT 'ccloud-infra-server', 'infra 服务', 0, NULL, NOW() FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'ccloud-infra-server');
INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
SELECT 'ccloud-open-server', 'open 服务', 0, NULL, NOW() FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'ccloud-open-server');
INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
SELECT 'ccloud-pay-server', 'pay 服务', 0, NULL, NOW() FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'ccloud-pay-server');
SET @system_group = (SELECT id FROM xxl_job_group WHERE app_name = 'ccloud-system-server');
SET @infra_group = (SELECT id FROM xxl_job_group WHERE app_name = 'ccloud-infra-server');
SET @open_group = (SELECT id FROM xxl_job_group WHERE app_name = 'ccloud-open-server');
SET @pay_group = (SELECT id FROM xxl_job_group WHERE app_name = 'ccloud-pay-server');

-- FIRST 路由 + 串行：多实例部署时同一时刻只有一个实例执行
INSERT INTO xxl_job_info (job_group, job_desc, add_time, update_time, author, alarm_email,
                          schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
                          executor_handler, executor_param, executor_block_strategy, executor_timeout,
                          executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
                          child_jobid, trigger_status)
SELECT t.job_group, t.job_desc, NOW(), NOW(), 'ccloud', '', 'CRON', t.cron, 'DO_NOTHING', 'FIRST',
       t.handler, '', 'SERIAL_EXECUTION', t.timeout, 0, 'BEAN', '', '', NOW(), '', 1
FROM (SELECT @system_group AS job_group, '消息发件箱重投（全部租户）' AS job_desc, '0 * * * * ?' AS cron, 'outboxRetryJob' AS handler, 60 AS timeout
      UNION ALL SELECT @system_group, 'Redis Stream 未确认消息重投', '30 * * * * ?', 'redisStreamPendingMessageResendJob', 60
      UNION ALL SELECT @infra_group, 'Redis Stream 未确认消息重投（日志）', '30 * * * * ?', 'redisStreamPendingMessageResendJob', 60
      UNION ALL SELECT @infra_group, '清理过期的 API 访问日志', '0 0 3 * * ?', 'apiAccessLogCleanJob', 3600
      UNION ALL SELECT @infra_group, '清理过期的 API 异常日志', '0 10 3 * * ?', 'apiErrorLogCleanJob', 3600
      UNION ALL SELECT @open_group, '回调通知发送与重试', '0/15 * * * * ?', 'openNotifyRetryJob', 60
      UNION ALL SELECT @open_group, '清理过期的调用日志与回调日志', '0 20 3 * * ?', 'openApiLogCleanJob', 3600
      UNION ALL SELECT @open_group, 'Redis Stream 未确认消息重投（调用日志）', '30 * * * * ?', 'redisStreamPendingMessageResendJob', 60
      UNION ALL SELECT @pay_group, '关闭超时未支付的订单', '0 * * * * ?', 'payOrderExpireJob', 60
      UNION ALL SELECT @pay_group, '补查等待中的支付', '10 * * * * ?', 'payOrderSyncJob', 60
      UNION ALL SELECT @pay_group, '补查等待中的退款', '20 * * * * ?', 'payRefundSyncJob', 60
      UNION ALL SELECT @pay_group, '补查未完成的转账', '40 * * * * ?', 'payTransferSyncJob', 60
      UNION ALL SELECT @pay_group, 'Redis Stream 未确认消息重投（支付消息）', '30 * * * * ?', 'redisStreamPendingMessageResendJob', 60) t
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_info i WHERE i.job_group = t.job_group AND i.executor_handler = t.handler);

UPDATE xxl_job_user SET password = '${password_hash}' WHERE username = 'admin';
SQL
echo "[04-ccloud-init] XXL-Job 执行器与任务已登记"

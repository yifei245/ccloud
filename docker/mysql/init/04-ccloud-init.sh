#!/bin/bash
# MySQL 容器首次初始化时执行（位于 01 ccloud 库、02 Nacos 库、03 XXL-Job 库之后）：
#   - XXL-Job：删除上游示例执行器与任务，登记 ccloud 的执行器与任务，并把默认管理员密码 123456 替换为 .env 中的密码
# 任务处理器名须与代码中的 @XxlJob 一致（system 的 MqJobHandler）
set -euo pipefail

: "${XXL_JOB_ADMIN_PASSWORD:?必须设置 XXL_JOB_ADMIN_PASSWORD}"
password_hash=$(printf '%s' "$XXL_JOB_ADMIN_PASSWORD" | sha256sum | cut -d' ' -f1)

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" xxl_job <<SQL
DELETE i FROM xxl_job_info i JOIN xxl_job_group g ON i.job_group = g.id
 WHERE g.app_name LIKE 'xxl-job-executor-sample%';
DELETE FROM xxl_job_group WHERE app_name LIKE 'xxl-job-executor-sample%';

-- address_type=0：执行器启动后自动注册地址。appname 与 xxl.job.executor.appname（即服务名）一致
INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
VALUES ('ccloud-system-server', 'system 服务', 0, NULL, NOW());
SET @group_id = LAST_INSERT_ID();

-- 每分钟执行；FIRST 路由 + 串行：多实例部署时同一时刻只有一个实例扫描，避免重复投递
INSERT INTO xxl_job_info (job_group, job_desc, add_time, update_time, author, alarm_email,
                          schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
                          executor_handler, executor_param, executor_block_strategy, executor_timeout,
                          executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
                          child_jobid, trigger_status)
VALUES (@group_id, '消息发件箱重投（全部租户）', NOW(), NOW(), 'ccloud', '',
        'CRON', '0 * * * * ?', 'DO_NOTHING', 'FIRST',
        'outboxRetryJob', '', 'SERIAL_EXECUTION', 60, 0, 'BEAN', '', '', NOW(), '', 1),
       (@group_id, 'Redis Stream 未确认消息重投', NOW(), NOW(), 'ccloud', '',
        'CRON', '30 * * * * ?', 'DO_NOTHING', 'FIRST',
        'redisStreamPendingMessageResendJob', '', 'SERIAL_EXECUTION', 60, 0, 'BEAN', '', '', NOW(), '', 1);

UPDATE xxl_job_user SET password = '${password_hash}' WHERE username = 'admin';
SQL
echo "[04-ccloud-init] XXL-Job 执行器与任务已登记"

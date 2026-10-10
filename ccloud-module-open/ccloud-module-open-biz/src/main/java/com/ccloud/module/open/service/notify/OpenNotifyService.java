package com.ccloud.module.open.service.notify;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.open.controller.admin.notify.vo.OpenNotifyLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.notify.OpenNotifyLogDO;

import java.util.Map;

/**
 * 开放应用的事件回调：写回调日志后异步发送，失败按退避策略重试
 */
public interface OpenNotifyService {

    /**
     * 向应用发送事件回调。记录属于应用所在的租户，调用方可在任意租户上下文中调用
     *
     * @return 回调日志编号；应用未配置回调地址时记录直接记为放弃
     */
    Long sendNotify(Long appId, String event, Map<String, Object> data);

    /**
     * 向当前租户的应用发送测试回调（事件 open.test）；应用未配置回调地址时报错
     *
     * @return 回调日志编号
     */
    Long sendTestNotify(Long appId);

    /**
     * 管理端手工重新通知：只限失败待重试与放弃的记录，重试次数清零后立即发送
     */
    void retryNotify(Long id);

    /**
     * 发送所有租户到期的回调（定时任务调用）
     *
     * @return 抢占并处理的条数
     */
    int executeDueNotifies(int limit);

    /**
     * 获得当前租户的回调日志
     */
    OpenNotifyLogDO getNotifyLog(Long id);

    /**
     * 获得当前租户的回调日志分页
     */
    PageResult<OpenNotifyLogDO> getNotifyLogPage(OpenNotifyLogPageReqVO reqVO);

    /**
     * 物理删除 N 天前已结束（成功、放弃）的回调日志，跨租户、分批
     *
     * @return 删除条数
     */
    int cleanNotifyLog(int retainDays, int batchSize);
}

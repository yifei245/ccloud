package com.ccloud.module.pay.service.common;

import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 渠道调用不能放在调用方的数据库事务里：渠道可能已经成功而本地记录随外层回滚消失，钱包渠道自带的本地事务也会误加入外层事务。
 * 本组件把渠道调用推迟到外层事务提交之后；没有外层事务时立即执行。
 * 任务在发起时的租户下、没有任何事务的环境中运行，任何异常（含 Error）都只记日志，不外抛：单据已落库，结果由补查任务确定
 */
@Component
@Slf4j
public class PayAfterCommitExecutor {

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 外层事务提交后触发任务时，那个已完成的事务仍绑在线程上：渠道（如钱包）自带的本地事务会误加入它而不再提交。
     * 因此任务挂起它，在没有任何事务的环境中执行
     */
    private TransactionTemplate noTransactionTemplate;

    @PostConstruct
    void initNoTransactionTemplate() {
        noTransactionTemplate = new TransactionTemplate(transactionTemplate.getTransactionManager());
        noTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    /**
     * @param description 任务描述，仅用于失败日志，如"退款 R2026..."
     * @param task        任务；在调用本方法时的租户下执行
     */
    public void execute(String description, Runnable task) {
        // 提交时线程上可能已没有租户上下文，所以在注册时捕获
        Long tenantId = TenantContextHolder.getTenantId();
        Runnable guarded = () -> {
            try {
                TenantUtils.execute(tenantId, () -> noTransactionTemplate.executeWithoutResult(status -> task.run()));
            } catch (Throwable t) {
                log.error("[execute] {} 提交后发起渠道调用失败，等待补查", description, t);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guarded.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guarded.run();
            }
        });
    }
}

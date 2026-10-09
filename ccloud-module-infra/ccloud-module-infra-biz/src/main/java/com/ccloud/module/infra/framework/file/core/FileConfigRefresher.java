package com.ccloud.module.infra.framework.file.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 文件配置的跨节点刷新。配置变更后发布消息，所有节点（包括发布者自己）收到后清空本地缓存的存储客户端
 */
@Slf4j
@Component
public class FileConfigRefresher implements MessageListener {

    public static final String CHANNEL = "ccloud:infra:file-config:refresh";

    private final StringRedisTemplate stringRedisTemplate;
    private final FileClientFactory fileClientFactory;

    public FileConfigRefresher(StringRedisTemplate stringRedisTemplate, FileClientFactory fileClientFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.fileClientFactory = fileClientFactory;
    }

    /**
     * 发布刷新消息。处于事务中时推迟到提交之后：提交前广播，其他节点可能重新加载到旧配置并再次缓存
     */
    public void publish() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stringRedisTemplate.convertAndSend(CHANNEL, "");
                }
            });
            return;
        }
        stringRedisTemplate.convertAndSend(CHANNEL, "");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        fileClientFactory.invalidateAll();
        log.debug("[onMessage][清空文件存储客户端缓存]");
    }
}

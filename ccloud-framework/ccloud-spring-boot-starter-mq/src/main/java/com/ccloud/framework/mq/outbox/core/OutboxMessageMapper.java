package com.ccloud.framework.mq.outbox.core;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxMessageMapper extends BaseMapperX<OutboxMessageDO> {

    /**
     * 捞出到期可重试的消息
     */
    default List<OutboxMessageDO> selectRetryable(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<OutboxMessageDO>()
                .in(OutboxMessageDO::getStatus,
                        List.of(OutboxStatusEnum.PENDING.getStatus(), OutboxStatusEnum.FAILURE.getStatus()))
                .le(OutboxMessageDO::getNextRetryTime, now)
                .orderByAsc(OutboxMessageDO::getId)
                .last("LIMIT " + limit));
    }
}

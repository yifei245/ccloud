package com.ccloud.framework.mq.outbox.core;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 死信的租户取自原消息，写入时由调用方显式赋值；查询由平台管理员跨租户处理。
 * 因此整个 Mapper 不参与多租户拦截：调度线程没有租户上下文，也不能让当前上下文覆盖原消息的租户
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface DeadLetterMapper extends BaseMapperX<DeadLetterDO> {
}

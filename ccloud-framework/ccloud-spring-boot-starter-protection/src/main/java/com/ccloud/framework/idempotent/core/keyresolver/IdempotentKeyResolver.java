package com.ccloud.framework.idempotent.core.keyresolver;

import com.ccloud.framework.idempotent.core.annotation.Idempotent;
import org.aspectj.lang.JoinPoint;

/**
 * 幂等键生成策略
 */
public interface IdempotentKeyResolver {

    String resolver(JoinPoint joinPoint, Idempotent idempotent);
}

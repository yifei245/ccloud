package com.ccloud.framework.security.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.Assert;

/**
 * 基于 {@link TransmittableThreadLocal} 的安全上下文策略。
 *
 * <p>Spring Security 默认的 ThreadLocal 策略在异步场景下拿不到身份，
 * 而 {@code @Async} 方法里调用 {@code @PreAuthorize} 的情况相当常见。
 * 与租户上下文保持同一套传递机制，两者行为一致。
 */
public class TransmittableThreadLocalSecurityContextHolderStrategy implements SecurityContextHolderStrategy {

    private static final ThreadLocal<SecurityContext> CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    @Override
    public void clearContext() {
        CONTEXT_HOLDER.remove();
    }

    @Override
    public SecurityContext getContext() {
        SecurityContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            context = createEmptyContext();
            CONTEXT_HOLDER.set(context);
        }
        return context;
    }

    @Override
    public void setContext(SecurityContext context) {
        Assert.notNull(context, "安全上下文不能为 null");
        CONTEXT_HOLDER.set(context);
    }

    @Override
    public SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }
}

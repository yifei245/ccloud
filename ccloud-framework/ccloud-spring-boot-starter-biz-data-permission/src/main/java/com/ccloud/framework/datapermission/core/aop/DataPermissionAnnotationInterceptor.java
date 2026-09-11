package com.ccloud.framework.datapermission.core.aop;

import com.ccloud.framework.datapermission.core.annotation.DataPermission;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRuleFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把方法上的 {@link DataPermission} 推入上下文，方法结束后弹出
 */
public class DataPermissionAnnotationInterceptor implements MethodInterceptor {

    /**
     * 延迟获取，避免与规则 Bean 形成初始化循环
     */
    private final ObjectProvider<DataPermissionRuleFactory> ruleFactoryProvider;

    public DataPermissionAnnotationInterceptor(ObjectProvider<DataPermissionRuleFactory> ruleFactoryProvider) {
        this.ruleFactoryProvider = ruleFactoryProvider;
    }

    /**
     * 注解解析结果缓存。方法与注解的对应关系不会变，没必要每次调用都反射。
     */
    private final Map<Method, DataPermission> annotationCache = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        DataPermission dataPermission = findAnnotation(invocation);
        if (dataPermission == null) {
            return invocation.proceed();
        }
        DataPermissionContextHolder.add(dataPermission);
        try {
            return invocation.proceed();
        } finally {
            DataPermissionContextHolder.remove();
            // 退出最外层时清理请求级缓存。容器线程会被复用，
            // 不清理会让下一个请求沿用上一个用户的数据权限
            if (DataPermissionContextHolder.get() == null) {
                DataPermissionRuleFactory factory = ruleFactoryProvider.getIfAvailable();
                if (factory != null) {
                    factory.clearRequestCache();
                }
            }
        }
    }

    private DataPermission findAnnotation(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        return annotationCache.computeIfAbsent(method, m -> {
            DataPermission annotation = AnnotatedElementUtils.findMergedAnnotation(m, DataPermission.class);
            if (annotation != null) {
                return annotation;
            }
            Class<?> targetClass = invocation.getThis() != null
                    ? ClassUtils.getUserClass(invocation.getThis()) : m.getDeclaringClass();
            return AnnotatedElementUtils.findMergedAnnotation(targetClass, DataPermission.class);
        });
    }
}

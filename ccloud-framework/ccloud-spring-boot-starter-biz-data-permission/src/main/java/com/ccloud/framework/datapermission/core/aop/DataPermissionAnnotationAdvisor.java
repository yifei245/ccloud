package com.ccloud.framework.datapermission.core.aop;

import com.ccloud.framework.datapermission.core.annotation.DataPermission;
import com.ccloud.framework.datapermission.core.rule.DataPermissionRuleFactory;
import org.springframework.aop.Pointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

/**
 * {@link DataPermission} 的切面通知器
 */
public class DataPermissionAnnotationAdvisor extends AbstractPointcutAdvisor {

    private final org.aopalliance.aop.Advice advice;

    public DataPermissionAnnotationAdvisor(ObjectProvider<DataPermissionRuleFactory> ruleFactoryProvider) {
        this.advice = new DataPermissionAnnotationInterceptor(ruleFactoryProvider);
    }

    private final Pointcut pointcut = AnnotationMatchingPointcut.forMethodAnnotation(DataPermission.class);

    @Override
    public org.aopalliance.aop.Advice getAdvice() {
        return advice;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
}

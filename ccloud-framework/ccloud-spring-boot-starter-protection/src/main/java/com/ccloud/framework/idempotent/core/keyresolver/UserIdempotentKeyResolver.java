package com.ccloud.framework.idempotent.core.keyresolver;

import cn.hutool.crypto.digest.DigestUtil;
import com.ccloud.framework.idempotent.core.annotation.Idempotent;
import com.ccloud.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;

/**
 * 在默认策略基础上带上当前用户。
 *
 * <p>不带用户维度时，两个用户提交相同参数会被误判为重复请求——
 * 例如两人同时领取同一张优惠券。
 */
public class UserIdempotentKeyResolver implements IdempotentKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
        String argsText = Arrays.toString(joinPoint.getArgs());
        Long userId = WebFrameworkUtils.getLoginUserId();
        Integer userType = WebFrameworkUtils.getLoginUserType();
        return DigestUtil.md5Hex(methodName + argsText + userId + userType);
    }
}

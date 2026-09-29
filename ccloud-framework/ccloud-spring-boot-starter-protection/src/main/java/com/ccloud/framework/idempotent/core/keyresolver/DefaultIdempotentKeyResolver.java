package com.ccloud.framework.idempotent.core.keyresolver;

import cn.hutool.crypto.digest.DigestUtil;
import com.ccloud.framework.idempotent.core.annotation.Idempotent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;

/**
 * 方法签名 + 全部入参。适用于"同样的参数就是同一次操作"的场景。
 */
public class DefaultIdempotentKeyResolver implements IdempotentKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
        String argsText = Arrays.toString(joinPoint.getArgs());
        return DigestUtil.md5Hex(methodName + argsText);
    }
}

package com.ccloud.framework.test.core.ut;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 纯 Mockito 单测基类。不启动 Spring 容器，适合验证不依赖数据库的业务规则。
 *
 * <p>能用它就别用 {@link BaseDbUnitTest}：连数据库的测试慢一到两个数量级，
 * 几百个这样的测试会让本地反馈循环变得难以忍受。
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseMockitoUnitTest {
}

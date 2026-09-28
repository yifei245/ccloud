package com.ccloud.framework.job.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * XxlJobSpringExecutor 在 afterSingletonsInstantiated 中自行 start、实现 DisposableBean 自行 destroy。
 * 若 @Bean 再声明 initMethod = "start"，内嵌服务会启动两次：第一次还早于任务处理器登记，
 * 第二次绑定端口失败并打印 BindException 错误日志，注册线程也起了两个
 */
@ExtendWith(OutputCaptureExtension.class)
class CcloudJobAutoConfigurationTest {

    private static final String START_SUCCESS = "xxl-job remoting server start success";

    @Test
    void 执行器内嵌服务只启动一次_关闭后端口应释放(CapturedOutput output) throws IOException {
        int port = freePort();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CcloudJobAutoConfiguration.class))
                .withPropertyValues(
                        "xxl.job.enabled=true",
                        "xxl.job.admin.addresses=http://127.0.0.1:1/xxl-job-admin",
                        "xxl.job.access-token=test",
                        "xxl.job.executor.appname=ccloud-test",
                        "xxl.job.executor.ip=127.0.0.1",
                        "xxl.job.executor.port=" + port,
                        "xxl.job.executor.log-path=" + System.getProperty("java.io.tmpdir") + "/ccloud-xxl-test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    await().atMost(Duration.ofSeconds(5)).until(() -> !isFree(port));
                    // 第二次启动的绑定失败发生在异步线程中，稍等再检查日志
                    Thread.sleep(1_000);
                });

        await().atMost(Duration.ofSeconds(10)).until(() -> isFree(port));
        assertThat(output.getAll().split(START_SUCCESS, -1)).as("内嵌服务启动次数").hasSize(2);
        assertThat(output.getAll()).doesNotContain("Address already in use");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static boolean isFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("0.0.0.0", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

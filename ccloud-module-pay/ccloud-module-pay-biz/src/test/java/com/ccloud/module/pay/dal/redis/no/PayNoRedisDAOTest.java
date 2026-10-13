package com.ccloud.module.pay.dal.redis.no;

import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PayNoRedisDAO.class)
class PayNoRedisDAOTest extends BasePayDbAndRedisUnitTest {

    @Resource
    private PayNoRedisDAO payNoRedisDAO;

    @Test
    void 单号为前缀加时间加六位序号() {
        assertThat(payNoRedisDAO.generate(PayNoRedisDAO.ORDER_PREFIX)).matches("^P\\d{14}\\d{6}$");
    }

    @Test
    void 并发生成不重复() throws Exception {
        Set<String> nos = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(pool.submit(() -> { for (int j = 0; j < 50; j++) nos.add(payNoRedisDAO.generate("R")); }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        assertThat(nos).hasSize(400);
    }
}

package com.ccloud.module.member.service.point;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.member.dal.dataobject.point.MemberPointRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.point.MemberPointRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

import static com.ccloud.module.member.enums.ErrorCodeConstants.USER_POINT_NOT_ENOUGH;
import static com.ccloud.module.member.enums.point.MemberPointBizTypeEnum.ORDER_USE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发扣积分：依赖会员行锁，不会扣成负数。不在测试事务中运行，数据会真实提交，结束时自行清理
 */
@Import(MemberPointServiceImpl.class)
class MemberPointConcurrencyTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberPointServiceImpl pointService;
    @Resource
    private MemberUserMapper userMapper;
    @Resource
    private MemberPointRecordMapper pointRecordMapper;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发扣积分_不会扣成负数_记录与余额一致() throws Exception {
        MemberUserDO user = new MemberUserDO().setMobile("13900009002").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("并发").setAvatar("").setPoint(100).setExperience(0);
        userMapper.insert(user);
        Long userId = user.getId();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                String bizId = "order-" + i;
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        TenantUtils.execute(TENANT_ID,
                                () -> pointService.reducePoint(userId, 10, ORDER_USE.getType(), bizId));
                        return 0;
                    } catch (ServiceException e) {
                        return e.getCode();
                    }
                }));
            }
            start.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : futures) {
                results.add(f.get(30, TimeUnit.SECONDS));
            }

            assertThat(results).filteredOn(code -> code == 0).hasSize(10);
            assertThat(results).filteredOn(code -> code != 0).hasSize(6)
                    .containsOnly(USER_POINT_NOT_ENOUGH.getCode());
            assertThat(userMapper.selectById(userId).getPoint()).isZero();
            assertThat(pointRecordMapper.selectList(MemberPointRecordDO::getUserId, userId).stream()
                    .sorted(Comparator.comparing(MemberPointRecordDO::getId))
                    .map(MemberPointRecordDO::getTotalPoint).toList())
                    .containsExactly(90, 80, 70, 60, 50, 40, 30, 20, 10, 0);
        } finally {
            pool.shutdownNow();
            pointRecordMapper.delete(MemberPointRecordDO::getUserId, userId);
            userMapper.deleteById(userId);
        }
    }
}

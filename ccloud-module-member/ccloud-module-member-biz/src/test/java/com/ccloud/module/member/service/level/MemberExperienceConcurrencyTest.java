package com.ccloud.module.member.service.level;

import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.member.dal.dataobject.level.MemberExperienceRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.level.MemberExperienceRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.ccloud.module.member.enums.experience.MemberExperienceBizTypeEnum.ORDER_GIVE_CANCEL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发扣经验：依赖会员行锁，同一会员的变动排队执行。不在测试事务中运行，数据会真实提交，结束时自行清理
 */
@Import(MemberLevelServiceImpl.class)
class MemberExperienceConcurrencyTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberLevelServiceImpl levelService;
    @Resource
    private MemberUserMapper userMapper;
    @Resource
    private MemberExperienceRecordMapper experienceRecordMapper;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发扣经验与重复业务编号_结果与记录一致() throws Exception {
        MemberUserDO user = new MemberUserDO().setMobile("13900009001").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("并发").setAvatar("").setPoint(0).setExperience(100);
        userMapper.insert(user);
        Long userId = user.getId();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String bizId = "cancel-" + (i % 10);   // 10 个不同编号，每个提交两次
                futures.add(pool.submit(() -> {
                    start.await();
                    TenantUtils.execute(TENANT_ID,
                            () -> levelService.reduceExperience(userId, 7, ORDER_GIVE_CANCEL.getType(), bizId));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }

            assertThat(userMapper.selectById(userId).getExperience()).isEqualTo(30);
            List<MemberExperienceRecordDO> records = experienceRecordMapper.selectList(MemberExperienceRecordDO::getUserId, userId);
            assertThat(records).hasSize(10);
            assertThat(records.stream().sorted((a, b) -> a.getId().compareTo(b.getId()))
                    .map(MemberExperienceRecordDO::getTotalExperience).toList())
                    .containsExactly(93, 86, 79, 72, 65, 58, 51, 44, 37, 30);
        } finally {
            pool.shutdownNow();
            experienceRecordMapper.delete(MemberExperienceRecordDO::getUserId, userId);
            userMapper.deleteById(userId);
        }
    }
}

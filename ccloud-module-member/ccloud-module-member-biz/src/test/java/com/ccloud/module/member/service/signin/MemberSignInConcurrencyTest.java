package com.ccloud.module.member.service.signin;

import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.member.dal.dataobject.point.MemberPointRecordDO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import com.ccloud.module.member.dal.dataobject.signin.MemberSignInRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.point.MemberPointRecordMapper;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInConfigMapper;
import com.ccloud.module.member.dal.mysql.signin.MemberSignInRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.service.level.MemberLevelServiceImpl;
import com.ccloud.module.member.service.point.MemberPointServiceImpl;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.ccloud.module.member.enums.ErrorCodeConstants.SIGN_IN_RECORD_TODAY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发签到（连点、多端同时签到）：依赖会员行锁，每天只签一次。不在测试事务中运行，结束时自行清理
 */
@Import({MemberSignInRecordServiceImpl.class, MemberSignInConfigServiceImpl.class,
        MemberPointServiceImpl.class, MemberLevelServiceImpl.class})
class MemberSignInConcurrencyTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberSignInRecordServiceImpl signInService;
    @Resource
    private MemberSignInConfigMapper configMapper;
    @Resource
    private MemberSignInRecordMapper recordMapper;
    @Resource
    private MemberPointRecordMapper pointRecordMapper;
    @Resource
    private MemberUserMapper userMapper;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 并发签到只成功一次_积分只发一次() throws Exception {
        MemberSignInConfigDO config = new MemberSignInConfigDO().setDay(1).setPoint(5).setExperience(0).setStatus(0);
        configMapper.insert(config);
        MemberUserDO user = new MemberUserDO().setMobile("13900009003").setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("并发").setAvatar("").setPoint(0).setExperience(0);
        userMapper.insert(user);
        Long userId = user.getId();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        TenantUtils.execute(TENANT_ID, () -> signInService.createSignRecord(userId));
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

            assertThat(results).filteredOn(code -> code == 0).hasSize(1);
            assertThat(results).filteredOn(code -> code != 0).hasSize(7)
                    .containsOnly(SIGN_IN_RECORD_TODAY_EXISTS.getCode());
            assertThat(recordMapper.selectList(MemberSignInRecordDO::getUserId, userId)).hasSize(1);
            assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(5);
        } finally {
            pool.shutdownNow();
            pointRecordMapper.delete(MemberPointRecordDO::getUserId, userId);
            recordMapper.delete(MemberSignInRecordDO::getUserId, userId);
            userMapper.deleteById(userId);
            configMapper.deleteById(config.getId());
        }
    }
}

package com.ccloud.module.member.service.point;

import com.ccloud.module.member.controller.admin.point.vo.MemberPointRecordPageReqVO;
import com.ccloud.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import com.ccloud.module.member.dal.dataobject.point.MemberPointRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.point.MemberPointRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.enums.point.MemberPointBizTypeEnum;
import com.ccloud.module.member.test.BaseMemberDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;
import static com.ccloud.module.member.enums.point.MemberPointBizTypeEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

@Import(MemberPointServiceImpl.class)
class MemberPointServiceImplTest extends BaseMemberDbUnitTest {

    @Resource
    private MemberPointServiceImpl pointService;
    @Resource
    private MemberPointRecordMapper pointRecordMapper;
    @Resource
    private MemberUserMapper userMapper;

    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        userId = insertUser("13900000001", 100);
        otherUserId = insertUser("13900000002", 0);
    }

    @Test
    void addPoint_记录变动与余额() {
        pointService.addPoint(userId, 20, SIGN.getType(), "sign-1");

        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(120);
        MemberPointRecordDO record = pointRecordMapper.selectByBiz(userId, SIGN.getType(), "sign-1");
        assertThat(record.getPoint()).isEqualTo(20);
        assertThat(record.getTotalPoint()).isEqualTo(120);
        assertThat(record.getTitle()).isEqualTo("签到");
        assertThat(record.getDescription()).isEqualTo("签到获得 20 积分");
    }

    @Test
    void reducePoint_余额不足时拒绝且不写记录() {
        assertServiceException(() -> pointService.reducePoint(userId, 101, ORDER_USE.getType(), "o-1"),
                USER_POINT_NOT_ENOUGH);
        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(100);
        assertThat(pointRecordMapper.selectCount()).isZero();
    }

    @Test
    void reducePoint_恰好扣完() {
        pointService.reducePoint(userId, 100, ORDER_USE.getType(), "o-1");

        assertThat(userMapper.selectById(userId).getPoint()).isZero();
        MemberPointRecordDO record = pointRecordMapper.selectByBiz(userId, ORDER_USE.getType(), "o-1");
        assertThat(record.getPoint()).isEqualTo(-100);
        assertThat(record.getTotalPoint()).isZero();
        assertThat(record.getDescription()).isEqualTo("下单使用 100 积分");
    }

    @Test
    void 同一业务编号只生效一次_不同业务类型互不影响() {
        pointService.addPoint(userId, 10, ORDER_GIVE.getType(), "o-1");
        pointService.addPoint(userId, 10, ORDER_GIVE.getType(), "o-1");
        pointService.reducePoint(userId, 5, ORDER_USE.getType(), "o-1");

        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(105);
        assertThat(pointRecordMapper.selectCount()).isEqualTo(2);
    }

    @Test
    void 已处理的扣减重复调用时即使余额不足也返回成功() {
        pointService.reducePoint(userId, 80, ORDER_USE.getType(), "o-1");
        pointService.reducePoint(userId, 80, ORDER_USE.getType(), "o-1");
        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(20);
    }

    @Test
    void 业务类型方向不符或数量不合法() {
        assertServiceException(() -> pointService.addPoint(userId, 1, ORDER_USE.getType(), "x"), POINT_BIZ_TYPE_INVALID);
        assertServiceException(() -> pointService.reducePoint(userId, 1, SIGN.getType(), "x"), POINT_BIZ_TYPE_INVALID);
        assertServiceException(() -> pointService.addPoint(userId, 1, 999, "x"), POINT_BIZ_TYPE_INVALID);
        assertServiceException(() -> pointService.addPoint(userId, -1, SIGN.getType(), "x"), BAD_REQUEST);
        assertServiceException(() -> pointService.reducePoint(userId, 0, ORDER_USE.getType(), "x"), BAD_REQUEST);
        assertThat(pointRecordMapper.selectCount()).isZero();
    }

    @Test
    void 会员不存在() {
        assertServiceException(() -> pointService.addPoint(-1L, 1, SIGN.getType(), "x"), USER_NOT_EXISTS);
    }

    @Test
    void updateUserPointByAdmin_正加负减_每次都生效_不能减到负数() {
        pointService.updateUserPointByAdmin(userId, 30);
        pointService.updateUserPointByAdmin(userId, 30);
        pointService.updateUserPointByAdmin(userId, -60);

        assertThat(userMapper.selectById(userId).getPoint()).isEqualTo(100);
        assertThat(pointRecordMapper.selectList()).extracting(MemberPointRecordDO::getDescription)
                .containsExactly("管理员增加 30 积分", "管理员增加 30 积分", "管理员扣减 60 积分");
        assertThat(pointRecordMapper.selectList()).extracting(MemberPointRecordDO::getBizType)
                .containsOnly(MemberPointBizTypeEnum.ADMIN.getType());
        assertServiceException(() -> pointService.updateUserPointByAdmin(userId, -101), USER_POINT_NOT_ENOUGH);
        assertServiceException(() -> pointService.updateUserPointByAdmin(userId, 0), BAD_REQUEST);
    }

    @Test
    void getPointRecordPage_用户端按收入支出筛选_只看本人_按时间倒序() {
        pointService.addPoint(userId, 10, SIGN.getType(), "s-1");
        pointService.reducePoint(userId, 5, ORDER_USE.getType(), "o-1");
        pointService.addPoint(userId, 3, ORDER_GIVE.getType(), "o-2");
        pointService.addPoint(otherUserId, 10, SIGN.getType(), "s-1");

        AppMemberPointRecordPageReqVO reqVO = new AppMemberPointRecordPageReqVO();
        reqVO.setAddStatus(true);
        assertThat(pointService.getPointRecordPage(userId, reqVO).getList()).extracting(MemberPointRecordDO::getPoint)
                .containsExactly(3, 10);
        reqVO.setAddStatus(false);
        assertThat(pointService.getPointRecordPage(userId, reqVO).getList()).extracting(MemberPointRecordDO::getPoint)
                .containsExactly(-5);
        reqVO.setAddStatus(null);
        assertThat(pointService.getPointRecordPage(userId, reqVO).getTotal()).isEqualTo(3);
    }

    @Test
    void getPointRecordPage_管理端按会员与业务类型筛选() {
        pointService.addPoint(userId, 10, SIGN.getType(), "s-1");
        pointService.addPoint(userId, 3, ORDER_GIVE.getType(), "o-2");
        pointService.addPoint(otherUserId, 10, SIGN.getType(), "s-1");

        MemberPointRecordPageReqVO reqVO = new MemberPointRecordPageReqVO();
        reqVO.setUserId(userId);
        assertThat(pointService.getPointRecordPage(reqVO).getTotal()).isEqualTo(2);
        reqVO.setBizType(SIGN.getType());
        assertThat(pointService.getPointRecordPage(reqVO).getList()).extracting(MemberPointRecordDO::getPoint)
                .containsExactly(10);
        reqVO.setUserId(null);
        reqVO.setBizType(null);
        reqVO.setTitle("订单");
        assertThat(pointService.getPointRecordPage(reqVO).getList()).extracting(MemberPointRecordDO::getPoint)
                .containsExactly(3);
    }

    private Long insertUser(String mobile, int point) {
        MemberUserDO user = new MemberUserDO().setMobile(mobile).setPassword("").setStatus(0)
                .setRegisterIp("127.0.0.1").setNickname("甲").setAvatar("").setPoint(point).setExperience(0);
        userMapper.insert(user);
        return user.getId();
    }
}

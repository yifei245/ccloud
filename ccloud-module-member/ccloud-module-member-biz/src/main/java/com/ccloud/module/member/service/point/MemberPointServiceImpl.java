package com.ccloud.module.member.service.point;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.member.controller.admin.point.vo.MemberPointRecordPageReqVO;
import com.ccloud.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import com.ccloud.module.member.dal.dataobject.point.MemberPointRecordDO;
import com.ccloud.module.member.dal.dataobject.user.MemberUserDO;
import com.ccloud.module.member.dal.mysql.point.MemberPointRecordMapper;
import com.ccloud.module.member.dal.mysql.user.MemberUserMapper;
import com.ccloud.module.member.enums.point.MemberPointBizTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.ccloud.module.member.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MemberPointServiceImpl implements MemberPointService {

    @Resource
    private MemberPointRecordMapper pointRecordMapper;
    @Resource
    private MemberUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoint(Long userId, Integer point, Integer bizType, String bizId) {
        changePoint(userId, point, validateBizType(point, bizType, true), bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reducePoint(Long userId, Integer point, Integer bizType, String bizId) {
        changePoint(userId, -point, validateBizType(point, bizType, false), bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPointByAdmin(Long userId, Integer point) {
        if (point == null || point == 0) {
            throw invalidParamException("调整的积分不能为 0");
        }
        // 管理员每次调整都应生效，业务编号随机生成
        changePoint(userId, point, MemberPointBizTypeEnum.ADMIN, IdUtil.fastSimpleUUID());
    }

    private static MemberPointBizTypeEnum validateBizType(Integer point, Integer bizType, boolean add) {
        if (point == null || point <= 0) {
            throw invalidParamException("积分必须大于 0");
        }
        MemberPointBizTypeEnum type = MemberPointBizTypeEnum.valueOf(bizType);
        if (type == null || type.isAdd() != add) {
            throw exception(POINT_BIZ_TYPE_INVALID);
        }
        return type;
    }

    /**
     * 锁定会员行后查重、改余额、写记录：同一会员的积分变动排队执行，并发扣减不会扣成负数
     */
    private void changePoint(Long userId, int delta, MemberPointBizTypeEnum type, String bizId) {
        MemberUserDO user = userId == null ? null : userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (pointRecordMapper.selectByBiz(userId, type.getType(), bizId) != null) {
            return;
        }
        int total = ObjectUtil.defaultIfNull(user.getPoint(), 0) + delta;
        if (total < 0) {
            throw exception(USER_POINT_NOT_ENOUGH);
        }
        userMapper.updateById(new MemberUserDO().setId(userId).setPoint(total));
        pointRecordMapper.insert(new MemberPointRecordDO().setUserId(userId).setBizType(type.getType())
                .setBizId(bizId).setTitle(type.getName()).setDescription(buildDescription(type, delta))
                .setPoint(delta).setTotalPoint(total));
    }

    /**
     * 管理员调整有增有减，业务类型的模板只适合增加，按正负号单独生成
     */
    private static String buildDescription(MemberPointBizTypeEnum type, int delta) {
        if (type == MemberPointBizTypeEnum.ADMIN) {
            return (delta > 0 ? "管理员增加 " : "管理员扣减 ") + Math.abs(delta) + " 积分";
        }
        return StrUtil.format(type.getDescription(), Math.abs(delta));
    }

    @Override
    public PageResult<MemberPointRecordDO> getPointRecordPage(MemberPointRecordPageReqVO reqVO) {
        return pointRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<MemberPointRecordDO> getPointRecordPage(Long userId, AppMemberPointRecordPageReqVO reqVO) {
        return pointRecordMapper.selectPage(userId, reqVO);
    }
}

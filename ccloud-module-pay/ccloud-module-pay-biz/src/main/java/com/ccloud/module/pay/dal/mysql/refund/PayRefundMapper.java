package com.ccloud.module.pay.dal.mysql.refund;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.controller.admin.refund.vo.PayRefundPageReqVO;
import com.ccloud.module.pay.dal.dataobject.refund.PayRefundDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PayRefundMapper extends BaseMapperX<PayRefundDO> {

    default PayRefundDO selectByAppIdAndMerchantRefundId(Long appId, String merchantRefundId) {
        return selectOne(PayRefundDO::getAppId, appId, PayRefundDO::getMerchantRefundId, merchantRefundId);
    }

    default PayRefundDO selectByNo(String no) {
        return selectOne(PayRefundDO::getNo, no);
    }

    default Long selectCountByOrderIdAndStatus(Long orderId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<PayRefundDO>().eq(PayRefundDO::getOrderId, orderId).eq(PayRefundDO::getStatus, status));
    }

    default Long selectCountByChannelIdAndStatus(Long channelId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<PayRefundDO>().eq(PayRefundDO::getChannelId, channelId).eq(PayRefundDO::getStatus, status));
    }

    default int updateByIdAndStatus(Long id, Integer whereStatus, PayRefundDO update) {
        return update(update, new LambdaUpdateWrapper<PayRefundDO>().eq(PayRefundDO::getId, id).eq(PayRefundDO::getStatus, whereStatus));
    }

    default PageResult<PayRefundDO> selectPage(PayRefundPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayRefundDO>()
                .eqIfPresent(PayRefundDO::getAppId, reqVO.getAppId())
                .eqIfPresent(PayRefundDO::getChannelCode, reqVO.getChannelCode())
                .eqIfPresent(PayRefundDO::getMerchantOrderId, reqVO.getMerchantOrderId())
                .eqIfPresent(PayRefundDO::getMerchantRefundId, reqVO.getMerchantRefundId())
                .eqIfPresent(PayRefundDO::getNo, reqVO.getNo())
                .eqIfPresent(PayRefundDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayRefundDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayRefundDO::getId));
    }

    /**
     * 按编号游标分页：只返回编号大于 afterId 的记录，调用方逐页推进，避免长期失败的记录占住队首
     */
    default List<PayRefundDO> selectListByStatusAndCreateTimeLt(Integer status, LocalDateTime time, Long afterId, int limit) {
        return selectList(new LambdaQueryWrapperX<PayRefundDO>().eq(PayRefundDO::getStatus, status)
                .gt(PayRefundDO::getId, afterId).lt(PayRefundDO::getCreateTime, time)
                .orderByAsc(PayRefundDO::getId).last("LIMIT " + limit));
    }
}

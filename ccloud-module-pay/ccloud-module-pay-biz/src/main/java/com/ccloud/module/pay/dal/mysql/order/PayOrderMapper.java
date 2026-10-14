package com.ccloud.module.pay.dal.mysql.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.controller.admin.order.vo.PayOrderPageReqVO;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PayOrderMapper extends BaseMapperX<PayOrderDO> {

    default PayOrderDO selectByAppIdAndMerchantOrderId(Long appId, String merchantOrderId) {
        return selectOne(PayOrderDO::getAppId, appId, PayOrderDO::getMerchantOrderId, merchantOrderId);
    }

    default PageResult<PayOrderDO> selectPage(PayOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayOrderDO>()
                .eqIfPresent(PayOrderDO::getAppId, reqVO.getAppId())
                .eqIfPresent(PayOrderDO::getChannelCode, reqVO.getChannelCode())
                .eqIfPresent(PayOrderDO::getMerchantOrderId, reqVO.getMerchantOrderId())
                .eqIfPresent(PayOrderDO::getNo, reqVO.getNo())
                .eqIfPresent(PayOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayOrderDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer whereStatus, PayOrderDO update) {
        return update(update, new LambdaUpdateWrapper<PayOrderDO>().eq(PayOrderDO::getId, id).eq(PayOrderDO::getStatus, whereStatus));
    }

    default List<PayOrderDO> selectListByStatusAndExpireTimeLt(Integer status, LocalDateTime time, int limit) {
        return selectList(new LambdaQueryWrapperX<PayOrderDO>().eq(PayOrderDO::getStatus, status)
                .lt(PayOrderDO::getExpireTime, time).orderByAsc(PayOrderDO::getId).last("LIMIT " + limit));
    }

    default Long selectCountByAppId(Long appId) {
        return selectCount(PayOrderDO::getAppId, appId);
    }

    /**
     * 预占退款额度：只有支付成功的订单，且累计退款加本次不超过支付金额时才成功
     */
    @Update("UPDATE pay_order SET refund_price = refund_price + #{price} WHERE id = #{id} AND deleted = 0 "
            + "AND status = 10 AND refund_price + #{price} <= price")
    int reserveRefundPrice(@Param("id") Long id, @Param("price") int price);

    /**
     * 退款失败时释放预占的额度
     */
    @Update("UPDATE pay_order SET refund_price = refund_price - #{price} WHERE id = #{id} AND deleted = 0 AND refund_price >= #{price}")
    int releaseRefundPrice(@Param("id") Long id, @Param("price") int price);
}

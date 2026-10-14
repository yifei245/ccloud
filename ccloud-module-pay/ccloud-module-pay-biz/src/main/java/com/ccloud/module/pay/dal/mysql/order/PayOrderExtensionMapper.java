package com.ccloud.module.pay.dal.mysql.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.dal.dataobject.order.PayOrderExtensionDO;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PayOrderExtensionMapper extends BaseMapperX<PayOrderExtensionDO> {

    default PayOrderExtensionDO selectByNo(String no) {
        return selectOne(PayOrderExtensionDO::getNo, no);
    }

    default List<PayOrderExtensionDO> selectListByOrderId(Long orderId) {
        return selectList(PayOrderExtensionDO::getOrderId, orderId);
    }

    default List<PayOrderExtensionDO> selectListByOrderIdAndStatus(Long orderId, Integer status) {
        return selectList(new LambdaQueryWrapperX<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getOrderId, orderId)
                .eq(PayOrderExtensionDO::getStatus, status));
    }

    default int updateByIdAndStatus(Long id, Integer whereStatus, PayOrderExtensionDO update) {
        return update(update, new LambdaUpdateWrapper<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getId, id)
                .eq(PayOrderExtensionDO::getStatus, whereStatus));
    }

    default List<PayOrderExtensionDO> selectListByStatusAndCreateTimeBetween(Integer status, LocalDateTime from,
                                                                            LocalDateTime to, int limit) {
        return selectList(new LambdaQueryWrapperX<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getStatus, status)
                .between(PayOrderExtensionDO::getCreateTime, from, to).orderByAsc(PayOrderExtensionDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 异常支付：支付尝试成功，但订单记录的成功尝试不是它（重复支付，或订单关闭后才付款成功）
     */
    default PageResult<PayOrderExtensionDO> selectAbnormalPage(PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<PayOrderExtensionDO>()
                .eq(PayOrderExtensionDO::getStatus, PayOrderStatusEnum.SUCCESS.getStatus())
                .apply("NOT EXISTS (SELECT 1 FROM pay_order o WHERE o.id = pay_order_extension.order_id "
                        + "AND o.extension_id = pay_order_extension.id AND o.deleted = 0)")
                .orderByDesc(PayOrderExtensionDO::getId));
    }
}

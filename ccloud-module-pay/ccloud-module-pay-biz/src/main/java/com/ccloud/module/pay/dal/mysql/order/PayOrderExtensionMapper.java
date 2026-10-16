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

    /**
     * 渠道名下仍有结果可期的等待中的支付尝试：订单也还在待支付。订单已支付或已关闭后遗留的兄弟尝试永远不会再有结果，不计入
     */
    default Long selectCountWaitingByChannelIdOfWaitingOrder(Long channelId) {
        return selectCount(new LambdaQueryWrapperX<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getChannelId, channelId)
                .eq(PayOrderExtensionDO::getStatus, PayOrderStatusEnum.WAITING.getStatus())
                .apply("EXISTS (SELECT 1 FROM pay_order o WHERE o.id = pay_order_extension.order_id AND o.status = {0} AND o.deleted = 0)",
                        PayOrderStatusEnum.WAITING.getStatus()));
    }

    default int updateByIdAndStatus(Long id, Integer whereStatus, PayOrderExtensionDO update) {
        return update(update, new LambdaUpdateWrapper<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getId, id)
                .eq(PayOrderExtensionDO::getStatus, whereStatus));
    }

    /**
     * 条件更新：仅当当前状态不是 notStatus 时更新。用于"成功"不可被覆盖、但可由等待或关闭转入的场景
     */
    default int updateByIdAndStatusNot(Long id, Integer notStatus, PayOrderExtensionDO update) {
        return update(update, new LambdaUpdateWrapper<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getId, id)
                .ne(PayOrderExtensionDO::getStatus, notStatus));
    }

    /**
     * 按编号游标分页：只返回编号大于 afterId 的记录，调用方逐页推进，避免长期失败的记录占住队首
     */
    default List<PayOrderExtensionDO> selectListByStatusAndCreateTimeBetween(Integer status, LocalDateTime from,
                                                                            LocalDateTime to, Long afterId, int limit) {
        return selectList(new LambdaQueryWrapperX<PayOrderExtensionDO>().eq(PayOrderExtensionDO::getStatus, status)
                .gt(PayOrderExtensionDO::getId, afterId).between(PayOrderExtensionDO::getCreateTime, from, to).orderByAsc(PayOrderExtensionDO::getId)
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

package com.ccloud.module.pay.dal.mysql.transfer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.pay.controller.admin.transfer.vo.PayTransferPageReqVO;
import com.ccloud.module.pay.dal.dataobject.transfer.PayTransferDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface PayTransferMapper extends BaseMapperX<PayTransferDO> {

    default PayTransferDO selectByAppIdAndMerchantTransferId(Long appId, String merchantTransferId) {
        return selectOne(PayTransferDO::getAppId, appId, PayTransferDO::getMerchantTransferId, merchantTransferId);
    }

    default PayTransferDO selectByNo(String no) {
        return selectOne(PayTransferDO::getNo, no);
    }

    default Long selectCountByChannelIdAndStatuses(Long channelId, Collection<Integer> statuses) {
        return selectCount(new LambdaQueryWrapperX<PayTransferDO>().eq(PayTransferDO::getChannelId, channelId)
                .in(PayTransferDO::getStatus, statuses));
    }

    default Long selectCountByAppIdAndStatuses(Long appId, Collection<Integer> statuses) {
        return selectCount(new LambdaQueryWrapperX<PayTransferDO>().eq(PayTransferDO::getAppId, appId)
                .in(PayTransferDO::getStatus, statuses));
    }

    default int updateByIdAndStatus(Long id, Collection<Integer> whereStatuses, PayTransferDO update) {
        return update(update, new LambdaUpdateWrapper<PayTransferDO>().eq(PayTransferDO::getId, id)
                .in(PayTransferDO::getStatus, whereStatuses));
    }

    default PageResult<PayTransferDO> selectPage(PayTransferPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayTransferDO>()
                .eqIfPresent(PayTransferDO::getAppId, reqVO.getAppId())
                .eqIfPresent(PayTransferDO::getChannelCode, reqVO.getChannelCode())
                .eqIfPresent(PayTransferDO::getMerchantTransferId, reqVO.getMerchantTransferId())
                .eqIfPresent(PayTransferDO::getNo, reqVO.getNo())
                .eqIfPresent(PayTransferDO::getUserAccount, reqVO.getUserAccount())
                .eqIfPresent(PayTransferDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayTransferDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayTransferDO::getId));
    }

    /**
     * 按编号游标分页：只返回编号大于 afterId 的记录（升序，最多 limit 条），调用方逐页推进，避免长期失败的记录占住队首
     */
    default List<PayTransferDO> selectListByStatusAndCreateTimeLt(Collection<Integer> statuses, LocalDateTime time,
                                                                  Long afterId, int limit) {
        return selectList(new LambdaQueryWrapperX<PayTransferDO>().in(PayTransferDO::getStatus, statuses)
                .gt(PayTransferDO::getId, afterId).lt(PayTransferDO::getCreateTime, time)
                .orderByAsc(PayTransferDO::getId).last("LIMIT " + limit));
    }
}

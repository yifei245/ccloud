package com.ccloud.module.open.dal.mysql.notify;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.open.controller.admin.notify.vo.OpenNotifyLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.notify.OpenNotifyLogDO;
import com.ccloud.module.open.enums.notify.OpenNotifyStatusEnum;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface OpenNotifyLogMapper extends BaseMapperX<OpenNotifyLogDO> {

    default PageResult<OpenNotifyLogDO> selectPage(OpenNotifyLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OpenNotifyLogDO>()
                .eqIfPresent(OpenNotifyLogDO::getAppId, reqVO.getAppId())
                .eqIfPresent(OpenNotifyLogDO::getEvent, reqVO.getEvent())
                .eqIfPresent(OpenNotifyLogDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(OpenNotifyLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(OpenNotifyLogDO::getId));
    }

    /**
     * 抢占一条到期的回调：把下次重试时间推到租约结束。多个实例、定时任务与即时发送并发时只有一方更新成功。
     * retryCount 为抢占前读到的值，保证抢占方据以计算退避的次数就是被抢占时的次数
     */
    @Update("UPDATE open_notify_log SET next_retry_time = #{lease}, last_notify_time = #{now} "
            + "WHERE id = #{id} AND status IN (0, 20) AND retry_count = #{retryCount} AND next_retry_time <= #{now} AND deleted = 0")
    int claim(@Param("id") Long id, @Param("retryCount") Integer retryCount,
              @Param("now") LocalDateTime now, @Param("lease") LocalDateTime lease);

    /**
     * 发送结果的回写，仅在仍持有租约时生效：租约结束时间（整秒，即库中的值）与抢占时的重试次数作为 fencing token。
     * 租约过期后被他方重新抢占、发送中被手工重新通知（次数清零）、已被他方写成成功或放弃时，影响 0 行
     */
    default int updateIfLeaseHeld(OpenNotifyLogDO update, Long id, LocalDateTime lease, Integer retryCount) {
        return update(update, new LambdaQueryWrapperX<OpenNotifyLogDO>()
                .eq(OpenNotifyLogDO::getId, id)
                .eq(OpenNotifyLogDO::getNextRetryTime, lease)
                .eq(OpenNotifyLogDO::getRetryCount, retryCount)
                .in(OpenNotifyLogDO::getStatus, OpenNotifyStatusEnum.WAITING.getStatus(), OpenNotifyStatusEnum.FAILURE.getStatus()));
    }

    /**
     * 只在状态属于 statuses 时更新，返回影响行数
     */
    default int updateByIdAndStatus(Long id, Collection<Integer> statuses, OpenNotifyLogDO update) {
        return update(update, new LambdaQueryWrapperX<OpenNotifyLogDO>()
                .eq(OpenNotifyLogDO::getId, id)
                .in(OpenNotifyLogDO::getStatus, statuses));
    }

    @Select("SELECT id FROM open_notify_log WHERE status IN (0, 20) AND next_retry_time <= #{now} AND deleted = 0 "
            + "ORDER BY next_retry_time LIMIT #{limit}")
    List<Long> selectDueIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 物理删除已结束（成功、放弃）的过期记录
     */
    @Delete("DELETE FROM open_notify_log WHERE status IN (10, 30) AND create_time < #{before} LIMIT #{limit}")
    int deleteFinishedByCreateTimeLt(@Param("before") LocalDateTime before, @Param("limit") int limit);
}

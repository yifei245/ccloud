package com.ccloud.module.open.dal.mysql.notify;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.open.controller.admin.notify.vo.OpenNotifyLogPageReqVO;
import com.ccloud.module.open.dal.dataobject.notify.OpenNotifyLogDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
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
     * 抢占一条到期的回调：把下次重试时间推到租约结束。多个实例、定时任务与即时发送并发时只有一方更新成功
     */
    @Update("UPDATE open_notify_log SET next_retry_time = #{lease}, last_notify_time = #{now} "
            + "WHERE id = #{id} AND status IN (0, 20) AND next_retry_time <= #{now} AND deleted = 0")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("lease") LocalDateTime lease);

    @Select("SELECT id FROM open_notify_log WHERE status IN (0, 20) AND next_retry_time <= #{now} AND deleted = 0 "
            + "ORDER BY next_retry_time LIMIT #{limit}")
    List<Long> selectDueIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 物理删除已结束（成功、放弃）的过期记录
     */
    @Delete("DELETE FROM open_notify_log WHERE status IN (10, 30) AND create_time < #{before} LIMIT #{limit}")
    int deleteFinishedByCreateTimeLt(@Param("before") LocalDateTime before, @Param("limit") int limit);
}

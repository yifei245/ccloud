package com.ccloud.module.infra.service.mq;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mq.outbox.core.DeadLetterDO;
import com.ccloud.framework.mq.outbox.core.DeadLetterMapper;
import com.ccloud.framework.mq.outbox.core.MessageSender;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.infra.controller.admin.mq.vo.MessageDeadLetterPageReqVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.MESSAGE_DEAD_LETTER_HANDLED;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.MESSAGE_DEAD_LETTER_NOT_EXISTS;

/**
 * DeadLetterMapper 整体不参与多租户拦截，这里的查询与更新天然跨租户
 */
@Service
public class MessageDeadLetterServiceImpl implements MessageDeadLetterService {

    @Resource
    private DeadLetterMapper deadLetterMapper;
    @Resource
    private MessageSender messageSender;

    @Override
    public PageResult<DeadLetterDO> getDeadLetterPage(MessageDeadLetterPageReqVO reqVO) {
        return deadLetterMapper.selectPage(reqVO, new LambdaQueryWrapperX<DeadLetterDO>()
                .likeIfPresent(DeadLetterDO::getTopic, reqVO.getTopic())
                .eqIfPresent(DeadLetterDO::getMessageId, reqVO.getMessageId())
                .eqIfPresent(DeadLetterDO::getHandled, reqVO.getHandled())
                .betweenIfPresent(DeadLetterDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DeadLetterDO::getId));
    }

    @Override
    public void updateDeadLetterHandled(Long id) {
        validateUnhandled(id);
        markHandled(id);
    }

    /**
     * 沿用原消息编号：消费方按编号幂等，已成功消费过的下游不会重复执行业务
     */
    @Override
    public void resendDeadLetter(Long id) {
        DeadLetterDO deadLetter = validateUnhandled(id);
        messageSender.send(deadLetter.getTopic(), deadLetter.getMessageId(), deadLetter.getPayload());
        markHandled(id);
    }

    private DeadLetterDO validateUnhandled(Long id) {
        DeadLetterDO deadLetter = deadLetterMapper.selectById(id);
        if (deadLetter == null) {
            throw exception(MESSAGE_DEAD_LETTER_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(deadLetter.getHandled())) {
            throw exception(MESSAGE_DEAD_LETTER_HANDLED);
        }
        return deadLetter;
    }

    private void markHandled(Long id) {
        DeadLetterDO update = new DeadLetterDO();
        update.setId(id);
        update.setHandled(true);
        update.setHandledTime(LocalDateTime.now());
        deadLetterMapper.updateById(update);
    }
}

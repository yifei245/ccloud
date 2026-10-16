package com.ccloud.module.pay.mq.consumer;

import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import org.springframework.stereotype.Component;

@Component
public class PayRefundSuccessListener extends AbstractPayRefundListener {

    public PayRefundSuccessListener() {
        super(PayRefundMessage.TOPIC_SUCCESS, OpenNotifyEvents.PAY_REFUND_SUCCESS, PayRefundStatusEnum.SUCCESS);
    }
}

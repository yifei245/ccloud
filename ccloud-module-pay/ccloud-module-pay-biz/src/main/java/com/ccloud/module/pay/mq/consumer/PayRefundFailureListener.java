package com.ccloud.module.pay.mq.consumer;

import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.mq.message.PayRefundMessage;
import org.springframework.stereotype.Component;

@Component
public class PayRefundFailureListener extends AbstractPayRefundListener {

    public PayRefundFailureListener() {
        super(PayRefundMessage.TOPIC_FAILURE, OpenNotifyEvents.PAY_REFUND_FAILURE);
    }
}

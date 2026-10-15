package com.ccloud.module.pay.mq.consumer;

import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.mq.message.PayTransferMessage;
import org.springframework.stereotype.Component;

@Component
public class PayTransferSuccessListener extends AbstractPayTransferListener {

    public PayTransferSuccessListener() {
        super(PayTransferMessage.TOPIC_SUCCESS, OpenNotifyEvents.PAY_TRANSFER_SUCCESS);
    }
}

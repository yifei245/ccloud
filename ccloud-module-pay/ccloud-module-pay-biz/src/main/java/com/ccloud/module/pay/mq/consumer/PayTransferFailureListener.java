package com.ccloud.module.pay.mq.consumer;

import com.ccloud.module.open.enums.notify.OpenNotifyEvents;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.mq.message.PayTransferMessage;
import org.springframework.stereotype.Component;

@Component
public class PayTransferFailureListener extends AbstractPayTransferListener {

    public PayTransferFailureListener() {
        super(PayTransferMessage.TOPIC_FAILURE, OpenNotifyEvents.PAY_TRANSFER_FAILURE, PayTransferStatusEnum.FAILURE);
    }
}

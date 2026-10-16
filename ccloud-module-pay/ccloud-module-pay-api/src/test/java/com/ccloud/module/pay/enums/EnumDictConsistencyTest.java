package com.ccloud.module.pay.enums;

import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 枚举值须与字典初始化数据一致：后台展示、导出按字典翻译，枚举多一个值就会显示成数字
 */
class EnumDictConsistencyTest {

    private static <E extends Enum<E>> Set<String> values(Class<E> type, Function<E, Object> getter) {
        return Arrays.stream(type.getEnumConstants()).map(getter).map(String::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("渠道编码与字典 pay_channel_code 一致")
    void channelCode() {
        assertThat(values(PayChannelEnum.class, PayChannelEnum::getCode))
                .isNotEmpty()
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues(PayChannelEnum.DICT_TYPE)));
    }

    @Test
    @DisplayName("支付单状态与字典 pay_order_status 一致")
    void orderStatus() {
        assertThat(values(PayOrderStatusEnum.class, PayOrderStatusEnum::getStatus))
                .isNotEmpty()
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues(PayOrderStatusEnum.DICT_TYPE)));
    }

    @Test
    @DisplayName("退款状态与字典 pay_refund_status 一致")
    void refundStatus() {
        assertThat(values(PayRefundStatusEnum.class, PayRefundStatusEnum::getStatus))
                .isNotEmpty()
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues(PayRefundStatusEnum.DICT_TYPE)));
    }

    @Test
    @DisplayName("转账状态与字典 pay_transfer_status 一致")
    void transferStatus() {
        assertThat(values(PayTransferStatusEnum.class, PayTransferStatusEnum::getStatus))
                .isNotEmpty()
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues(PayTransferStatusEnum.DICT_TYPE)));
    }

    @Test
    @DisplayName("钱包业务类型与字典 pay_wallet_biz_type 一致")
    void walletBizType() {
        assertThat(values(PayWalletBizTypeEnum.class, PayWalletBizTypeEnum::getType))
                .isNotEmpty()
                .isEqualTo(new TreeSet<>(SqlFixture.dictValues(PayWalletBizTypeEnum.DICT_TYPE)));
    }
}

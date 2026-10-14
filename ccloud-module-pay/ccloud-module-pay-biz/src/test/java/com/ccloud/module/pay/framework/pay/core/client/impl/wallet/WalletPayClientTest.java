package com.ccloud.module.pay.framework.pay.core.client.impl.wallet;

import com.ccloud.framework.common.enums.UserTypeEnum;
import com.ccloud.framework.common.exception.ServiceException;
import com.ccloud.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.ccloud.module.pay.dal.redis.no.PayNoRedisDAO;
import com.ccloud.module.pay.enums.wallet.PayWalletBizTypeEnum;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayClientStatus;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayDisplayMode;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayNotifyRequest;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderResult;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayOrderUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayRefundUnifiedReq;
import com.ccloud.module.pay.framework.pay.core.client.dto.PayTransferUnifiedReq;
import com.ccloud.module.pay.service.wallet.PayWalletServiceImpl;
import com.ccloud.module.pay.test.BasePayDbAndRedisUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static com.ccloud.module.pay.enums.ErrorCodeConstants.CHANNEL_OPERATION_NOT_SUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({PayWalletServiceImpl.class, PayNoRedisDAO.class, WalletPayClientProvider.class})
class WalletPayClientTest extends BasePayDbAndRedisUnitTest {

    @Resource
    private WalletPayClientProvider provider;
    @Resource
    private PayWalletServiceImpl walletService;

    private WalletPayClient client() {
        return (WalletPayClient) provider.create(1L, new WalletPayClientConfig());
    }

    private PayOrderUnifiedReq orderReq(String no, long userId, int price) {
        return new PayOrderUnifiedReq().setOutTradeNo(no).setSubject("测试").setPrice(price)
                .setUserId(userId).setUserType(UserTypeEnum.MEMBER.getValue());
    }

    @Test
    void 余额足够时直接支付成功_无需用户操作() {
        PayWalletDO wallet = walletService.getOrCreateWallet(71001L, 1);
        walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.ADJUST, "init", "初始", 1000);
        PayOrderResult result = client().unifiedOrder(orderReq("P71001", 71001L, 300));
        assertThat(result.getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        assertThat(result.getDisplayMode()).isEqualTo(PayDisplayMode.NONE);
        assertThat(result.getPrice()).isEqualTo(300);
        assertThat(client().getOrder("P71001").getStatus()).isEqualTo(PayClientStatus.SUCCESS);
    }

    @Test
    void 余额不足返回失败结果而不抛异常() {
        PayOrderResult result = client().unifiedOrder(orderReq("P71002", 71002L, 300));
        assertThat(result.getStatus()).isEqualTo(PayClientStatus.FAILURE);
        assertThat(result.getErrorMsg()).contains("余额不足");
        assertThat(client().getOrder("P71002").getStatus()).isEqualTo(PayClientStatus.WAITING);
    }

    @Test
    void 未指定会员时返回失败结果() {
        PayOrderResult result = client().unifiedOrder(orderReq("P71003", 0L, 300).setUserId(null));
        assertThat(result.getStatus()).isEqualTo(PayClientStatus.FAILURE);
    }

    @Test
    void 退款退回原钱包_重复退款只退一次() {
        PayWalletDO wallet = walletService.getOrCreateWallet(71004L, 1);
        walletService.addBalance(wallet.getId(), PayWalletBizTypeEnum.ADJUST, "init", "初始", 1000);
        client().unifiedOrder(orderReq("P71004", 71004L, 300));
        PayRefundUnifiedReq req = new PayRefundUnifiedReq().setOutTradeNo("P71004").setOutRefundNo("R71004")
                .setRefundPrice(100).setPayPrice(300).setReason("退");
        assertThat(client().unifiedRefund(req).getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        client().unifiedRefund(req);
        assertThat(walletService.getWallet(wallet.getId()).getBalance()).isEqualTo(800);
        assertThat(client().getRefund("P71004", "R71004").getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        assertThat(client().getRefund("P71004", "R-none").getStatus()).as("未入账返回等待，由补查重发").isEqualTo(PayClientStatus.WAITING);
    }

    @Test
    void 转账入账到收款会员的钱包_收款账号须为会员编号() {
        PayTransferUnifiedReq req = new PayTransferUnifiedReq().setOutTransferNo("T71005").setSubject("佣金")
                .setPrice(200).setUserAccount("71005");
        assertThat(client().unifiedTransfer(req).getStatus()).isEqualTo(PayClientStatus.SUCCESS);
        assertThat(walletService.getOrCreateWallet(71005L, 1).getBalance()).isEqualTo(200);
        assertThat(client().getTransfer("T71005").getStatus()).isEqualTo(PayClientStatus.SUCCESS);

        PayTransferUnifiedReq bad = new PayTransferUnifiedReq().setOutTransferNo("T71006").setPrice(1).setUserAccount("abc");
        assertThat(client().unifiedTransfer(bad).getStatus()).isEqualTo(PayClientStatus.FAILURE);
    }

    @Test
    void 钱包渠道没有回调() {
        assertThatThrownBy(() -> client().parseOrderNotify(new PayNotifyRequest(Map.of(), Map.of(), "{}")))
                .isInstanceOf(ServiceException.class).extracting("code").isEqualTo(CHANNEL_OPERATION_NOT_SUPPORTED.getCode());
    }
}

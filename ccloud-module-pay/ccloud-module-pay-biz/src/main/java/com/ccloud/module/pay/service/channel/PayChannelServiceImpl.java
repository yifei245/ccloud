package com.ccloud.module.pay.service.channel;

import cn.hutool.core.util.StrUtil;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.pay.controller.admin.channel.vo.PayChannelSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import com.ccloud.module.pay.dal.mysql.order.PayOrderExtensionMapper;
import com.ccloud.module.pay.dal.mysql.refund.PayRefundMapper;
import com.ccloud.module.pay.dal.mysql.transfer.PayTransferMapper;
import com.ccloud.module.pay.enums.PayChannelEnum;
import com.ccloud.module.pay.enums.order.PayOrderStatusEnum;
import com.ccloud.module.pay.enums.refund.PayRefundStatusEnum;
import com.ccloud.module.pay.enums.transfer.PayTransferStatusEnum;
import com.ccloud.module.pay.framework.config.PayProperties;
import com.ccloud.module.pay.framework.pay.core.client.PayClient;
import com.ccloud.module.pay.framework.pay.core.client.PayClientFactory;
import com.ccloud.module.pay.service.app.PayAppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;

@Service
@Validated
public class PayChannelServiceImpl implements PayChannelService {

    @Resource
    private PayChannelMapper payChannelMapper;
    @Resource
    private PayOrderExtensionMapper extensionMapper;
    @Resource
    private PayRefundMapper refundMapper;
    @Resource
    private PayTransferMapper transferMapper;
    @Resource
    private PayAppService payAppService;
    @Resource
    private PayClientFactory payClientFactory;
    @Resource
    private PayProperties payProperties;

    @Override
    public Long createChannel(PayChannelSaveReqVO reqVO) {
        if (payAppService.getApp(reqVO.getAppId()) == null) {
            throw exception(PAY_APP_NOT_FOUND);
        }
        validateCodeSupported(reqVO.getCode());
        if (payChannelMapper.selectByAppIdAndCode(reqVO.getAppId(), reqVO.getCode()) != null) {
            throw exception(CHANNEL_EXIST_SAME_CHANNEL_ERROR);
        }
        payClientFactory.parseConfig(reqVO.getCode(), reqVO.getConfig());
        PayChannelDO channel = new PayChannelDO().setAppId(reqVO.getAppId()).setCode(reqVO.getCode())
                .setStatus(reqVO.getStatus()).setFeeRate(reqVO.getFeeRate()).setRemark(reqVO.getRemark())
                .setConfig(reqVO.getConfig());
        payChannelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    public void updateChannel(PayChannelSaveReqVO reqVO) {
        PayChannelDO channel = validateChannelExists(reqVO.getId());
        if (CommonStatusEnum.ENABLE.getStatus().equals(reqVO.getStatus())) {
            validateCodeSupported(channel.getCode());
        }
        // 编码已不受支持（如遗留的 Mock 渠道）时只允许改状态、费率、备注，配置保持原样
        boolean supported = payClientFactory.isSupported(channel.getCode());
        String config = supported
                ? payClientFactory.mergeMaskedConfig(channel.getCode(), reqVO.getConfig(), channel.getConfig())
                : channel.getConfig();
        if (supported) {
            payClientFactory.parseConfig(channel.getCode(), config);
        }
        payChannelMapper.updateById(new PayChannelDO().setId(channel.getId()).setStatus(reqVO.getStatus())
                .setFeeRate(reqVO.getFeeRate()).setRemark(reqVO.getRemark()).setConfig(config));
        payClientFactory.invalidate(channel.getId());
    }

    @Override
    public void deleteChannel(Long id) {
        validateChannelExists(id);
        // 渠道删除后，未完成的单据无处补查、回调也找不到渠道：卡在等待中的钱无法了结
        if (extensionMapper.selectCountByChannelIdAndStatus(id, PayOrderStatusEnum.WAITING.getStatus()) > 0
                || refundMapper.selectCountByChannelIdAndStatus(id, PayRefundStatusEnum.WAITING.getStatus()) > 0
                || transferMapper.selectCountByChannelIdAndStatuses(id, List.of(PayTransferStatusEnum.WAITING.getStatus(),
                PayTransferStatusEnum.PROCESSING.getStatus())) > 0) {
            throw exception(CHANNEL_HAS_UNFINISHED);
        }
        payChannelMapper.deleteById(id);
        payClientFactory.invalidate(id);
    }

    /**
     * Mock 渠道由 ccloud.pay.mock-enabled 控制是否注册：未注册时给出专门的提示，便于定位环境配置
     */
    private void validateCodeSupported(String code) {
        PayChannelEnum channel = PayChannelEnum.getByCode(code);
        if (channel == PayChannelEnum.MOCK && !payClientFactory.isSupported(code)) {
            throw exception(CHANNEL_MOCK_DISABLED);
        }
        if (channel == null || !payClientFactory.isSupported(code)) {
            throw exception(CHANNEL_NOT_SUPPORTED, code);
        }
    }

    private PayChannelDO validateChannelExists(Long id) {
        PayChannelDO channel = payChannelMapper.selectById(id);
        if (channel == null) {
            throw exception(CHANNEL_NOT_FOUND);
        }
        return channel;
    }

    @Override
    public PayChannelDO getChannel(Long id) {
        return payChannelMapper.selectById(id);
    }

    @Override
    public List<PayChannelDO> getChannelListByAppId(Long appId) {
        return payChannelMapper.selectListByAppId(appId);
    }

    @Override
    public List<PayChannelDO> getEnableChannelList(Long appId) {
        return payChannelMapper.selectListByAppIdAndStatus(appId, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public PayChannelDO validPayChannel(Long appId, String code) {
        PayChannelDO channel = payChannelMapper.selectByAppIdAndCode(appId, code);
        if (channel == null) {
            throw exception(CHANNEL_NOT_FOUND);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(channel.getStatus())) {
            throw exception(CHANNEL_IS_DISABLE);
        }
        return channel;
    }

    @Override
    public PayChannelDO getChannelIgnoreTenant(Long id) {
        return TenantUtils.executeIgnore(() -> payChannelMapper.selectById(id));
    }

    @Override
    public PayClient getPayClient(PayChannelDO channel) {
        return payClientFactory.getPayClient(channel.getId(), channel.getCode(), channel.getConfig());
    }

    @Override
    public String buildNotifyUrl(String type, Long channelId) {
        return StrUtil.removeSuffix(payProperties.getNotifyBaseUrl(), "/") + "/app-api/pay/notify/" + type + "/" + channelId;
    }
}

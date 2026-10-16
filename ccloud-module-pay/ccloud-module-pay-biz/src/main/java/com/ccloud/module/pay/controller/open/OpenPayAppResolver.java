package com.ccloud.module.pay.controller.open;

import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.service.app.PayAppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.PAY_APP_NOT_OPENED_FOR_OPEN_APP;

/**
 * 开放接口的调用方解析：全部开放订单、退款接口经由这里取得支付应用。
 * 钱包充值应用不对开放平台开放：经开放接口退款会绕过充值退款的"先扣回余额再退款"，所以即使它被绑定了开放应用，
 * 也按"开放应用未开通支付"处理
 */
@Component
public class OpenPayAppResolver {

    @Resource
    private PayAppService appService;

    /**
     * 网关验签后下发的开放应用身份：登录编号即开放应用编号
     */
    public PayAppDO validApp() {
        PayAppDO app = appService.validPayAppByOpenAppId(getLoginUserId());
        if (appService.isRechargeApp(app)) {
            throw exception(PAY_APP_NOT_OPENED_FOR_OPEN_APP);
        }
        return app;
    }
}

package com.ccloud.module.pay.service.app;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppPageReqVO;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;

import java.util.List;

/**
 * 支付应用 Service
 */
public interface PayAppService {

    Long createApp(PayAppSaveReqVO reqVO);

    void updateApp(PayAppSaveReqVO reqVO);

    void deleteApp(Long id);

    PayAppDO getApp(Long id);

    PageResult<PayAppDO> getAppPage(PayAppPageReqVO reqVO);

    List<PayAppDO> getAppList();

    /**
     * 校验应用可用：不存在 PAY_APP_NOT_FOUND，禁用 PAY_APP_IS_DISABLE
     */
    PayAppDO validPayApp(Long id);

    /**
     * 按应用标识校验应用可用，规则同 {@link #validPayApp(Long)}
     */
    PayAppDO validPayApp(String appKey);

    /**
     * 开放应用调用支付开放接口时找到它绑定的支付应用：未绑定 PAY_APP_NOT_OPENED_FOR_OPEN_APP，禁用 PAY_APP_IS_DISABLE
     */
    PayAppDO validPayAppByOpenAppId(Long openAppId);

    /**
     * 忽略租户查询应用，供消息消费、回调转发使用
     */
    PayAppDO getAppIgnoreTenant(Long id);
}

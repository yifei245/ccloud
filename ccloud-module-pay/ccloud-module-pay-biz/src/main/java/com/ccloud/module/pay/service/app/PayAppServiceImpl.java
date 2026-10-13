package com.ccloud.module.pay.service.app;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.tenant.core.context.TenantContextHolder;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.open.api.app.OpenAppApi;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppPageReqVO;
import com.ccloud.module.pay.controller.admin.app.vo.PayAppSaveReqVO;
import com.ccloud.module.pay.dal.dataobject.app.PayAppDO;
import com.ccloud.module.pay.dal.dataobject.channel.PayChannelDO;
import com.ccloud.module.pay.dal.mysql.app.PayAppMapper;
import com.ccloud.module.pay.dal.mysql.channel.PayChannelMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.pay.enums.ErrorCodeConstants.*;

@Service
@Validated
public class PayAppServiceImpl implements PayAppService {

    @Resource
    private PayAppMapper payAppMapper;
    @Resource
    private PayChannelMapper payChannelMapper;
    @Resource
    private OpenAppApi openAppApi;

    @Override
    public Long createApp(PayAppSaveReqVO reqVO) {
        if (payAppMapper.selectByAppKey(reqVO.getAppKey()) != null) {
            throw exception(PAY_APP_KEY_EXISTS);
        }
        PayAppDO app = new PayAppDO().setAppKey(reqVO.getAppKey()).setName(reqVO.getName())
                .setStatus(reqVO.getStatus()).setRemark(reqVO.getRemark())
                .setOpenAppId(resolveOpenAppId(reqVO.getOpenAppKey(), null));
        payAppMapper.insert(app);
        return app.getId();
    }

    @Override
    public void updateApp(PayAppSaveReqVO reqVO) {
        PayAppDO app = validateAppExists(reqVO.getId());
        // 应用标识被内部服务与钱包充值引用，创建后不可修改
        payAppMapper.updateById(new PayAppDO().setId(app.getId()).setName(reqVO.getName()).setStatus(reqVO.getStatus())
                .setRemark(reqVO.getRemark()).setOpenAppId(resolveOpenAppId(reqVO.getOpenAppKey(), app.getId())));
        if (StrUtil.isBlank(reqVO.getOpenAppKey()) && app.getOpenAppId() != null) {
            // updateById 不更新 null 字段，解除绑定须显式置空
            payAppMapper.update(null, new LambdaUpdateWrapper<PayAppDO>().eq(PayAppDO::getId, app.getId())
                    .set(PayAppDO::getOpenAppId, null));
        }
    }

    /**
     * 开放应用须存在且属于当前租户，且未被其他支付应用绑定。
     * 快照中含开放应用密钥：这里只取编号与租户，不记录日志、不保存
     */
    private Long resolveOpenAppId(String openAppKey, Long selfId) {
        if (StrUtil.isBlank(openAppKey)) {
            return null;
        }
        OpenAppSnapshotRespDTO snapshot = openAppApi.getAppSnapshot(openAppKey).getCheckedData();
        if (snapshot == null || !Objects.equals(snapshot.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw exception(PAY_APP_OPEN_APP_INVALID);
        }
        PayAppDO bound = payAppMapper.selectByOpenAppId(snapshot.getId());
        if (bound != null && !Objects.equals(bound.getId(), selfId)) {
            throw exception(PAY_APP_OPEN_APP_BOUND);
        }
        return snapshot.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApp(Long id) {
        validateAppExists(id);
        // TODO Task 8：应用下存在支付单时拒绝删除（PAY_APP_EXIST_ORDER）
        // 渠道客户端缓存不必主动清理：取用时带着库中的配置，已删除渠道的缓存项不会再被访问
        payChannelMapper.delete(PayChannelDO::getAppId, id);
        payAppMapper.deleteById(id);
    }

    @Override
    public PayAppDO getApp(Long id) {
        return payAppMapper.selectById(id);
    }

    @Override
    public PageResult<PayAppDO> getAppPage(PayAppPageReqVO reqVO) {
        return payAppMapper.selectPage(reqVO);
    }

    @Override
    public List<PayAppDO> getAppList() {
        return payAppMapper.selectList();
    }

    @Override
    public PayAppDO validPayApp(Long id) {
        return validateEnabled(payAppMapper.selectById(id));
    }

    @Override
    public PayAppDO validPayApp(String appKey) {
        return validateEnabled(payAppMapper.selectByAppKey(appKey));
    }

    @Override
    public PayAppDO validPayAppByOpenAppId(Long openAppId) {
        PayAppDO app = payAppMapper.selectByOpenAppId(openAppId);
        if (app == null) {
            throw exception(PAY_APP_NOT_OPENED_FOR_OPEN_APP);
        }
        return validateEnabled(app);
    }

    @Override
    public PayAppDO getAppIgnoreTenant(Long id) {
        return TenantUtils.executeIgnore(() -> payAppMapper.selectById(id));
    }

    private PayAppDO validateAppExists(Long id) {
        PayAppDO app = payAppMapper.selectById(id);
        if (app == null) {
            throw exception(PAY_APP_NOT_FOUND);
        }
        return app;
    }

    private static PayAppDO validateEnabled(PayAppDO app) {
        if (app == null) {
            throw exception(PAY_APP_NOT_FOUND);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(app.getStatus())) {
            throw exception(PAY_APP_IS_DISABLE);
        }
        return app;
    }
}

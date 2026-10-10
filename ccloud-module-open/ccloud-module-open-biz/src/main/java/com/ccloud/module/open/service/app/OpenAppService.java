package com.ccloud.module.open.service.app;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.open.api.app.dto.OpenAppSnapshotRespDTO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppCreateRespVO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppPageReqVO;
import com.ccloud.module.open.controller.admin.app.vo.OpenAppSaveReqVO;
import com.ccloud.module.open.dal.dataobject.app.OpenAppDO;
import jakarta.validation.Valid;

import java.util.Set;

/**
 * 开放应用 Service
 */
public interface OpenAppService {

    /**
     * 创建应用，应用标识与密钥由系统生成
     *
     * @return 应用编号、标识与一次性返回的密钥
     */
    OpenAppCreateRespVO createApp(@Valid OpenAppSaveReqVO reqVO);

    /**
     * 修改应用。应用标识与密钥不可修改
     */
    void updateApp(@Valid OpenAppSaveReqVO reqVO);

    /**
     * 删除应用，同时删除它的接口授权
     */
    void deleteApp(Long id);

    /**
     * 获得当前租户的应用，不存在返回 null
     */
    OpenAppDO getApp(Long id);

    /**
     * 跨租户获得应用，日志与回调用；不存在返回 null
     */
    OpenAppDO getAppIgnoreTenant(Long id);

    PageResult<OpenAppDO> getAppPage(OpenAppPageReqVO reqVO);

    /**
     * 重置密钥，旧密钥立即失效
     *
     * @return 新密钥原文
     */
    String resetAppSecret(Long id);

    /**
     * 整体替换应用的接口授权，只能选择开启的接口
     */
    void assignApis(Long appId, Set<Long> apiIds);

    Set<Long> getAppApiIds(Long appId);

    /**
     * 按 appKey 跨租户获得应用快照，供网关使用；不存在返回 null
     */
    OpenAppSnapshotRespDTO getAppSnapshot(String appKey);
}

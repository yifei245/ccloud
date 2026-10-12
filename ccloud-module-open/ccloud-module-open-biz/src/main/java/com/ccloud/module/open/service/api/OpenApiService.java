package com.ccloud.module.open.service.api;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiPageReqVO;
import com.ccloud.module.open.controller.admin.api.vo.OpenApiSaveReqVO;
import com.ccloud.module.open.dal.dataobject.api.OpenApiDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 开放接口目录 Service。目录是全局数据，只有系统租户可以增删改
 */
public interface OpenApiService {

    /**
     * 创建接口
     *
     * @return 接口编号
     */
    Long createApi(@Valid OpenApiSaveReqVO reqVO);

    void updateApi(@Valid OpenApiSaveReqVO reqVO);

    /**
     * 删除接口，同时删除所有租户应用对它的授权
     */
    void deleteApi(Long id);

    OpenApiDO getApi(Long id);

    PageResult<OpenApiDO> getApiPage(OpenApiPageReqVO reqVO);

    /**
     * @return 所有开启的接口
     */
    List<OpenApiDO> getEnabledApiList();

    /**
     * @param method 大写的请求方法
     * @return 不存在返回 null
     */
    OpenApiDO getApiByMethodAndPath(String method, String path);
}

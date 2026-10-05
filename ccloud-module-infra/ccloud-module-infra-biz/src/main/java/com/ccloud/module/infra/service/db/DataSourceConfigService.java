package com.ccloud.module.infra.service.db;

import com.ccloud.module.infra.controller.admin.db.vo.DataSourceConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.db.DataSourceConfigDO;
import jakarta.validation.Valid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据源配置 Service。0 为应用自身的主数据源，取自应用配置，只读
 */
public interface DataSourceConfigService {

    Long MASTER_ID = 0L;

    /**
     * 创建数据源配置，连接测试通过才保存
     */
    Long createDataSourceConfig(@Valid DataSourceConfigSaveReqVO reqVO);

    /**
     * 修改数据源配置，密码提交掩码或为空表示沿用原密码
     */
    void updateDataSourceConfig(@Valid DataSourceConfigSaveReqVO reqVO);

    /**
     * 删除数据源配置，已有代码生成表引用时拒绝
     */
    void deleteDataSourceConfig(Long id);

    /**
     * @param id 编号，0 为主数据源
     * @return 数据源配置，不存在返回 null
     */
    DataSourceConfigDO getDataSourceConfig(Long id);

    /**
     * @return 全部数据源，主数据源在首位
     */
    List<DataSourceConfigDO> getDataSourceConfigList();

    /**
     * 打开到该数据源的连接，调用方负责关闭。0 为应用自身的主数据源
     */
    Connection openConnection(Long id) throws SQLException;
}

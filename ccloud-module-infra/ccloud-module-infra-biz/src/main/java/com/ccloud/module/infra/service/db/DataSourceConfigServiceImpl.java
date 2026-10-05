package com.ccloud.module.infra.service.db;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.framework.operatelog.core.util.DesensitizeUtils;
import com.ccloud.module.infra.controller.admin.db.vo.DataSourceConfigSaveReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.dal.dataobject.db.DataSourceConfigDO;
import com.ccloud.module.infra.dal.mysql.codegen.CodegenTableMapper;
import com.ccloud.module.infra.dal.mysql.db.DataSourceConfigMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.infra.enums.ErrorCodeConstants.*;

@Slf4j
@Service
@Validated
public class DataSourceConfigServiceImpl implements DataSourceConfigService {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;
    @Resource
    private CodegenTableMapper codegenTableMapper;
    @Resource
    private DynamicDataSourceProperties dynamicDataSourceProperties;
    @Resource
    private DataSource dataSource;

    @Override
    public Long createDataSourceConfig(DataSourceConfigSaveReqVO reqVO) {
        if (StrUtil.isEmpty(reqVO.getPassword())) {
            throw exception(DATA_SOURCE_CONFIG_NOT_OK);
        }
        DataSourceConfigDO config = BeanUtils.toBean(reqVO, DataSourceConfigDO.class).setId(null);
        validateConnectionOk(config);
        dataSourceConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateDataSourceConfig(DataSourceConfigSaveReqVO reqVO) {
        validateNotMaster(reqVO.getId());
        DataSourceConfigDO old = validateExists(reqVO.getId());
        DataSourceConfigDO config = BeanUtils.toBean(reqVO, DataSourceConfigDO.class);
        // 前端拿到的是掩码，原样提交表示不改密码
        if (StrUtil.isEmpty(config.getPassword()) || DesensitizeUtils.MASK.equals(config.getPassword())) {
            config.setPassword(old.getPassword());
        }
        validateConnectionOk(config);
        dataSourceConfigMapper.updateById(config);
    }

    @Override
    public void deleteDataSourceConfig(Long id) {
        validateNotMaster(id);
        validateExists(id);
        if (codegenTableMapper.selectCount(CodegenTableDO::getDataSourceConfigId, id) > 0) {
            throw exception(DATA_SOURCE_CONFIG_IN_USE);
        }
        dataSourceConfigMapper.deleteById(id);
    }

    @Override
    public DataSourceConfigDO getDataSourceConfig(Long id) {
        if (MASTER_ID.equals(id)) {
            return getMasterConfig();
        }
        return dataSourceConfigMapper.selectById(id);
    }

    @Override
    public List<DataSourceConfigDO> getDataSourceConfigList() {
        List<DataSourceConfigDO> list = new ArrayList<>();
        list.add(getMasterConfig());
        list.addAll(dataSourceConfigMapper.selectList());
        return list;
    }

    @Override
    public Connection openConnection(Long id) throws SQLException {
        if (MASTER_ID.equals(id)) {
            return dataSource.getConnection();
        }
        DataSourceConfigDO config = getDataSourceConfig(id);
        if (config == null) {
            throw exception(DATA_SOURCE_CONFIG_NOT_EXISTS);
        }
        return connect(config);
    }

    private DataSourceConfigDO getMasterConfig() {
        String primary = dynamicDataSourceProperties.getPrimary();
        DataSourceProperty master = dynamicDataSourceProperties.getDatasource().get(primary);
        return new DataSourceConfigDO().setId(MASTER_ID).setName(primary)
                .setUrl(master.getUrl()).setUsername(master.getUsername()).setPassword(master.getPassword());
    }

    private static Connection connect(DataSourceConfigDO config) throws SQLException {
        if (!StrUtil.startWith(config.getUrl(), "jdbc:mysql://")) {
            throw new SQLException("只支持 MySQL：" + config.getUrl());
        }
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", StrUtil.nullToEmpty(config.getPassword()));
        // 连不上时尽快失败，不让管理端请求挂住
        props.setProperty("connectTimeout", "5000");
        props.setProperty("socketTimeout", "30000");
        return DriverManager.getConnection(config.getUrl(), props);
    }

    private void validateConnectionOk(DataSourceConfigDO config) {
        try (Connection ignored = connect(config)) {
            // 能建立连接即可
        } catch (SQLException e) {
            // 只记录不含查询参数的地址，避免 url 里携带密码时写入日志
            log.warn("[validateConnectionOk][数据源({}) {} 连接失败]", config.getName(),
                    StrUtil.subBefore(config.getUrl(), "?", false), e);
            throw exception(DATA_SOURCE_CONFIG_NOT_OK);
        }
    }

    private static void validateNotMaster(Long id) {
        if (MASTER_ID.equals(id)) {
            throw exception(DATA_SOURCE_CONFIG_MASTER_READONLY);
        }
    }

    private DataSourceConfigDO validateExists(Long id) {
        DataSourceConfigDO config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw exception(DATA_SOURCE_CONFIG_NOT_EXISTS);
        }
        return config;
    }
}

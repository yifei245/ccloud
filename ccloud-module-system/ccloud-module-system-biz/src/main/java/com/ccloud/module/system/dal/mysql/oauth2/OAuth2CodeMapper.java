package com.ccloud.module.system.dal.mysql.oauth2;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权码自带租户归属，按授权码查询与作废时忽略租户
 */
@Mapper
public interface OAuth2CodeMapper extends BaseMapperX<OAuth2CodeDO> {

    default OAuth2CodeDO selectByCode(String code) {
        return TenantUtils.executeIgnore(() -> selectOne(OAuth2CodeDO::getCode, code));
    }

    /**
     * @return 实际作废的行数；并发消费同一授权码时只有一个请求得到 1
     */
    default int deleteByIdIgnoreTenant(Long id) {
        return TenantUtils.executeIgnore(() -> deleteById(id));
    }
}

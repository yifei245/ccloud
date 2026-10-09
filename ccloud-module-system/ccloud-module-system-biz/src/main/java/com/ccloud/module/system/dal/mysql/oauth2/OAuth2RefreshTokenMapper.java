package com.ccloud.module.system.dal.mysql.oauth2;

import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.framework.tenant.core.util.TenantUtils;
import com.ccloud.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷新令牌自带租户归属，按令牌查询时忽略租户
 */
@Mapper
public interface OAuth2RefreshTokenMapper extends BaseMapperX<OAuth2RefreshTokenDO> {

    default OAuth2RefreshTokenDO selectByRefreshToken(String refreshToken) {
        return TenantUtils.executeIgnore(() -> selectOne(OAuth2RefreshTokenDO::getRefreshToken, refreshToken));
    }

    default void deleteByRefreshToken(String refreshToken) {
        TenantUtils.executeIgnore(() -> delete(OAuth2RefreshTokenDO::getRefreshToken, refreshToken));
    }

    /**
     * 同一用户类型的用户编号全局唯一，忽略租户
     */
    default void deleteByUserIdAndUserTypeIgnoreTenant(Long userId, Integer userType) {
        TenantUtils.executeIgnore(() -> delete(new LambdaQueryWrapperX<OAuth2RefreshTokenDO>()
                .eq(OAuth2RefreshTokenDO::getUserId, userId)
                .eq(OAuth2RefreshTokenDO::getUserType, userType)));
    }
}

package com.ccloud.module.system.dal.mysql.i18n;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessagePageReqVO;
import com.ccloud.module.system.dal.dataobject.i18n.I18nMessageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface I18nMessageMapper extends BaseMapperX<I18nMessageDO> {

    default I18nMessageDO selectByLocaleAndCode(String locale, String code) {
        return selectOne(I18nMessageDO::getLocale, locale, I18nMessageDO::getCode, code);
    }

    default List<I18nMessageDO> selectListByLocale(String locale) {
        return selectList(I18nMessageDO::getLocale, locale);
    }

    default PageResult<I18nMessageDO> selectPage(I18nMessagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<I18nMessageDO>()
                .eqIfPresent(I18nMessageDO::getLocale, reqVO.getLocale())
                .likeIfPresent(I18nMessageDO::getCode, reqVO.getCode())
                .likeIfPresent(I18nMessageDO::getContent, reqVO.getContent())
                .eqIfPresent(I18nMessageDO::getSource, reqVO.getSource())
                .orderByAsc(I18nMessageDO::getCode)
                .orderByAsc(I18nMessageDO::getLocale));
    }
}

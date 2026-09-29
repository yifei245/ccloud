package com.ccloud.module.system.dal.mysql.mail;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.controller.admin.mail.vo.template.MailTemplatePageReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailTemplateMapper extends BaseMapperX<MailTemplateDO> {

    default MailTemplateDO selectByCode(String code) {
        return selectOne(MailTemplateDO::getCode, code);
    }

    default Long selectCountByAccountId(Long accountId) {
        return selectCount(MailTemplateDO::getAccountId, accountId);
    }

    default PageResult<MailTemplateDO> selectPage(MailTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MailTemplateDO>()
                .eqIfPresent(MailTemplateDO::getStatus, reqVO.getStatus())
                .likeIfPresent(MailTemplateDO::getCode, reqVO.getCode())
                .likeIfPresent(MailTemplateDO::getName, reqVO.getName())
                .eqIfPresent(MailTemplateDO::getAccountId, reqVO.getAccountId())
                .betweenIfPresent(MailTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MailTemplateDO::getId));
    }
}

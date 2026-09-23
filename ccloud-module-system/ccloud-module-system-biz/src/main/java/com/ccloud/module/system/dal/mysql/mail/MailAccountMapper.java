package com.ccloud.module.system.dal.mysql.mail;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.mybatis.core.mapper.BaseMapperX;
import com.ccloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.ccloud.module.system.controller.admin.mail.vo.account.MailAccountPageReqVO;
import com.ccloud.module.system.dal.dataobject.mail.MailAccountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailAccountMapper extends BaseMapperX<MailAccountDO> {

    default PageResult<MailAccountDO> selectPage(MailAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MailAccountDO>()
                .likeIfPresent(MailAccountDO::getMail, reqVO.getMail())
                .likeIfPresent(MailAccountDO::getUsername, reqVO.getUsername())
                .orderByDesc(MailAccountDO::getId));
    }
}

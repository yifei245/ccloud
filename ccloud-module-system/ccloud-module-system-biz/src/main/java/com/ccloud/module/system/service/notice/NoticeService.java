package com.ccloud.module.system.service.notice;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.module.system.controller.admin.notice.vo.NoticePageReqVO;
import com.ccloud.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import com.ccloud.module.system.dal.dataobject.notice.NoticeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 通知公告 Service
 */
public interface NoticeService {

    Long createNotice(@Valid NoticeSaveReqVO createReqVO);

    void updateNotice(@Valid NoticeSaveReqVO updateReqVO);

    void deleteNotice(Long id);

    void deleteNoticeList(List<Long> ids);

    NoticeDO getNotice(Long id);

    PageResult<NoticeDO> getNoticePage(NoticePageReqVO reqVO);
}

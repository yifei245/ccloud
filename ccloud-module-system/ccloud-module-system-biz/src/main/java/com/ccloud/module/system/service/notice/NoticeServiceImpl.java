package com.ccloud.module.system.service.notice;

import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.system.controller.admin.notice.vo.NoticePageReqVO;
import com.ccloud.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import com.ccloud.module.system.dal.dataobject.notice.NoticeDO;
import com.ccloud.module.system.dal.mysql.notice.NoticeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ccloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTICE_NOT_FOUND;

@Service
@Validated
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public Long createNotice(NoticeSaveReqVO createReqVO) {
        NoticeDO notice = BeanUtils.toBean(createReqVO, NoticeDO.class);
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    public void updateNotice(NoticeSaveReqVO updateReqVO) {
        validateNoticeExists(updateReqVO.getId());
        noticeMapper.updateById(BeanUtils.toBean(updateReqVO, NoticeDO.class));
    }

    @Override
    public void deleteNotice(Long id) {
        validateNoticeExists(id);
        noticeMapper.deleteById(id);
    }

    @Override
    public void deleteNoticeList(List<Long> ids) {
        noticeMapper.deleteByIds(ids);
    }

    private void validateNoticeExists(Long id) {
        if (noticeMapper.selectById(id) == null) {
            throw exception(NOTICE_NOT_FOUND);
        }
    }

    @Override
    public NoticeDO getNotice(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    public PageResult<NoticeDO> getNoticePage(NoticePageReqVO reqVO) {
        return noticeMapper.selectPage(reqVO);
    }
}

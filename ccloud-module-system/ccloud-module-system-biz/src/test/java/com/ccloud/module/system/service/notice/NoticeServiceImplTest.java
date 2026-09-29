package com.ccloud.module.system.service.notice;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.notice.vo.NoticePageReqVO;
import com.ccloud.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import com.ccloud.module.system.dal.dataobject.notice.NoticeDO;
import com.ccloud.module.system.dal.mysql.notice.NoticeMapper;
import com.ccloud.module.system.enums.notice.NoticeTypeEnum;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTICE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

@Import(NoticeServiceImpl.class)
class NoticeServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private NoticeService noticeService;
    @Resource
    private NoticeMapper noticeMapper;

    @Test
    void createNotice_成功且归属当前租户() {
        Long id = noticeService.createNotice(reqVO("系统升级"));
        NoticeDO notice = noticeMapper.selectById(id);
        assertThat(notice.getTitle()).isEqualTo("系统升级");
        assertThat(notice.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void updateNotice_成功与不存在() {
        Long id = noticeService.createNotice(reqVO("旧标题"));
        NoticeSaveReqVO reqVO = reqVO("新标题");
        reqVO.setId(id);
        noticeService.updateNotice(reqVO);
        assertThat(noticeMapper.selectById(id).getTitle()).isEqualTo("新标题");

        reqVO.setId(-1L);
        assertServiceException(() -> noticeService.updateNotice(reqVO), NOTICE_NOT_FOUND);
    }

    @Test
    void deleteNotice_成功与不存在() {
        Long id = noticeService.createNotice(reqVO("待删"));
        noticeService.deleteNotice(id);
        assertThat(noticeMapper.selectById(id)).isNull();
        assertServiceException(() -> noticeService.deleteNotice(-1L), NOTICE_NOT_FOUND);
    }

    @Test
    void getNoticePage_按标题与状态过滤() {
        noticeService.createNotice(reqVO("page-甲"));
        NoticeSaveReqVO closed = reqVO("page-乙");
        closed.setStatus(CommonStatusEnum.DISABLE.getStatus());
        noticeService.createNotice(closed);

        NoticePageReqVO reqVO = new NoticePageReqVO();
        reqVO.setTitle("page-");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        assertThat(noticeService.getNoticePage(reqVO).getList()).extracting(NoticeDO::getTitle).containsExactly("page-甲");
    }

    private static NoticeSaveReqVO reqVO(String title) {
        NoticeSaveReqVO reqVO = new NoticeSaveReqVO();
        reqVO.setTitle(title);
        reqVO.setContent("<p>内容</p>");
        reqVO.setType(NoticeTypeEnum.NOTICE.getType());
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}

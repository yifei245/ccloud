package com.ccloud.module.system.service.notify;

import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.module.system.controller.admin.notify.vo.template.NotifyTemplateSaveReqVO;
import com.ccloud.module.system.dal.mysql.notify.NotifyTemplateMapper;
import com.ccloud.module.system.enums.notify.NotifyTemplateTypeEnum;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_CODE_DUPLICATE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.NOTIFY_TEMPLATE_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

@Import(NotifyTemplateServiceImpl.class)
class NotifyTemplateServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private NotifyTemplateService templateService;
    @Resource
    private NotifyTemplateMapper templateMapper;

    @Test
    void createNotifyTemplate_成功时从内容解析参数() {
        Long id = templateService.createNotifyTemplate(reqVO("t-audit", "您的申请 {no} 已由 {auditor} 审批"));
        assertThat(templateMapper.selectById(id).getParams()).containsExactly("no", "auditor");
    }

    @Test
    void createNotifyTemplate_编码重复() {
        templateService.createNotifyTemplate(reqVO("dup", "x"));
        assertServiceException(() -> templateService.createNotifyTemplate(reqVO("dup", "y")), NOTIFY_TEMPLATE_CODE_DUPLICATE);
    }

    @Test
    void updateNotifyTemplate_成功与不存在与编码冲突() {
        templateService.createNotifyTemplate(reqVO("taken", "x"));
        Long id = templateService.createNotifyTemplate(reqVO("self", "x"));
        NotifyTemplateSaveReqVO reqVO = reqVO("self", "新内容 {a}");
        reqVO.setId(id);

        templateService.updateNotifyTemplate(reqVO);
        assertThat(templateMapper.selectById(id).getParams()).containsExactly("a");

        reqVO.setCode("taken");
        assertServiceException(() -> templateService.updateNotifyTemplate(reqVO), NOTIFY_TEMPLATE_CODE_DUPLICATE);
        reqVO.setId(-1L);
        assertServiceException(() -> templateService.updateNotifyTemplate(reqVO), NOTIFY_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void deleteNotifyTemplate_成功与不存在() {
        Long id = templateService.createNotifyTemplate(reqVO("del", "x"));
        templateService.deleteNotifyTemplate(id);
        assertThat(templateMapper.selectById(id)).isNull();
        assertServiceException(() -> templateService.deleteNotifyTemplate(-1L), NOTIFY_TEMPLATE_NOT_EXISTS);
    }

    @Test
    void getNotifyTemplateByCode_与内容渲染() {
        templateService.createNotifyTemplate(reqVO("t-code", "您好 {name}"));
        assertThat(templateService.getNotifyTemplateByCode("t-code").getName()).isEqualTo("模板t-code");
        assertThat(templateService.getNotifyTemplateByCode("none")).isNull();
        assertThat(templateService.formatNotifyTemplateContent("您好 {name}", Map.of("name", "张三"))).isEqualTo("您好 张三");
    }

    static NotifyTemplateSaveReqVO reqVO(String code, String content) {
        NotifyTemplateSaveReqVO reqVO = new NotifyTemplateSaveReqVO();
        reqVO.setName("模板" + code);
        reqVO.setCode(code);
        reqVO.setNickname("系统");
        reqVO.setContent(content);
        reqVO.setType(NotifyTemplateTypeEnum.SYSTEM_MESSAGE.getType());
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }
}

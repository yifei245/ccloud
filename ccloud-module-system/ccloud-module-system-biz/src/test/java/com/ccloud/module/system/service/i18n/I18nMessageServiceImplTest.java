package com.ccloud.module.system.service.i18n;

import com.ccloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.ccloud.framework.i18n.config.I18nProperties;
import com.ccloud.framework.i18n.core.I18nMessageRefresher;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessagePageReqVO;
import com.ccloud.module.system.controller.admin.i18n.vo.I18nMessageSaveReqVO;
import com.ccloud.module.system.dal.dataobject.i18n.I18nMessageDO;
import com.ccloud.module.system.dal.mysql.i18n.I18nMessageMapper;
import com.ccloud.module.system.test.BaseSystemDbUnitTest;
import com.ccloud.module.system.test.CacheTestConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static com.ccloud.module.system.enums.ErrorCodeConstants.I18N_MESSAGE_DUPLICATE;
import static com.ccloud.module.system.enums.ErrorCodeConstants.I18N_MESSAGE_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Import({I18nMessageServiceImpl.class, CacheTestConfiguration.class})
@EnableConfigurationProperties(I18nProperties.class)
class I18nMessageServiceImplTest extends BaseSystemDbUnitTest {

    @Resource
    private I18nMessageService messageService;
    @Resource
    private I18nMessageMapper messageMapper;

    @MockitoBean
    private I18nMessageRefresher refresher;

    @Test
    void createI18nMessage_成功后通知各服务刷新() {
        Long id = messageService.createI18nMessage(reqVO("zh_CN", "error.demo", "演示"));

        assertThat(messageMapper.selectById(id).getContent()).isEqualTo("演示");
        verify(refresher).publishRefresh();
    }

    @Test
    void createI18nMessage_同语言同编码重复应失败_不同语言允许() {
        messageService.createI18nMessage(reqVO("zh_CN", "error.dup", "中文"));
        messageService.createI18nMessage(reqVO("en_US", "error.dup", "English"));

        assertServiceException(() -> messageService.createI18nMessage(reqVO("zh_CN", "error.dup", "又一条")),
                I18N_MESSAGE_DUPLICATE);
    }

    @Test
    void createI18nMessage_不支持的语言应拒绝() {
        assertServiceException(() -> messageService.createI18nMessage(reqVO("fr_FR", "error.x", "x")),
                GlobalErrorCodeConstants.BAD_REQUEST);
    }

    @Test
    void updateI18nMessage_编码与同语言其他文案冲突应失败_不存在应失败() {
        messageService.createI18nMessage(reqVO("zh_CN", "taken", "x"));
        Long id = messageService.createI18nMessage(reqVO("zh_CN", "self", "x"));
        I18nMessageSaveReqVO reqVO = reqVO("zh_CN", "taken", "y");
        reqVO.setId(id);

        assertServiceException(() -> messageService.updateI18nMessage(reqVO), I18N_MESSAGE_DUPLICATE);
        reqVO.setId(-1L);
        assertServiceException(() -> messageService.updateI18nMessage(reqVO), I18N_MESSAGE_NOT_EXISTS);
    }

    @Test
    void getMessages_只返回指定语言的全部文案() {
        messageService.createI18nMessage(reqVO("zh_CN", "a", "甲"));
        messageService.createI18nMessage(reqVO("zh_CN", "b", "乙"));
        messageService.createI18nMessage(reqVO("en_US", "a", "A"));

        assertThat(messageService.getMessages("zh_CN")).isEqualTo(Map.of("a", "甲", "b", "乙"));
        assertThat(messageService.getMessages("en_US")).isEqualTo(Map.of("a", "A"));
        assertThat(messageService.getMessages("ja_JP")).isEmpty();
    }

    @Test
    void 更新后缓存被清除_再读到的是新文案() {
        Long id = messageService.createI18nMessage(reqVO("zh_CN", "a", "旧"));
        assertThat(messageService.getMessages("zh_CN")).containsEntry("a", "旧");

        I18nMessageSaveReqVO reqVO = reqVO("zh_CN", "a", "新");
        reqVO.setId(id);
        messageService.updateI18nMessage(reqVO);
        assertThat(messageService.getMessages("zh_CN")).containsEntry("a", "新");

        messageService.deleteI18nMessage(id);
        assertThat(messageService.getMessages("zh_CN")).doesNotContainKey("a");
        verify(refresher, times(3)).publishRefresh();
    }

    @Test
    void getMessages_命中缓存时不查库() {
        messageService.createI18nMessage(reqVO("zh_CN", "a", "甲"));
        messageService.getMessages("zh_CN");
        // 绕过服务直接改库：缓存未清除时仍返回旧值，说明第二次读走了缓存
        I18nMessageDO row = messageMapper.selectList().get(0);
        messageMapper.updateById(new I18nMessageDO().setId(row.getId()).setContent("直接改库"));

        assertThat(messageService.getMessages("zh_CN")).containsEntry("a", "甲");
    }

    @Test
    void getI18nMessagePage_按语言与编码过滤() {
        messageService.createI18nMessage(reqVO("zh_CN", "error.page.a", "x"));
        messageService.createI18nMessage(reqVO("en_US", "error.page.b", "y"));
        I18nMessagePageReqVO reqVO = new I18nMessagePageReqVO();
        reqVO.setLocale("zh_CN");
        reqVO.setCode("error.page");

        assertThat(messageService.getI18nMessagePage(reqVO).getList()).extracting(I18nMessageDO::getCode)
                .containsExactly("error.page.a");
    }

    private static I18nMessageSaveReqVO reqVO(String locale, String code, String content) {
        I18nMessageSaveReqVO reqVO = new I18nMessageSaveReqVO();
        reqVO.setLocale(locale);
        reqVO.setCode(code);
        reqVO.setContent(content);
        return reqVO;
    }
}

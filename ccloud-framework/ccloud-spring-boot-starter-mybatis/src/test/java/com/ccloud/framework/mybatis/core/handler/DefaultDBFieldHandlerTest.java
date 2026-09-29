package com.ccloud.framework.mybatis.core.handler;

import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDBFieldHandlerTest {

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class DemoDO extends BaseDO {
        private Long id;
    }

    private MetaObject metaOf(Object obj) {
        return SystemMetaObject.forObject(obj);
    }

    @Test
    @DisplayName("insert 时应填充创建时间、更新时间、创建者、更新者与删除标记")
    void insertFill_shouldPopulateAuditFields() {
        DefaultDBFieldHandler handler = new DefaultDBFieldHandler(() -> 1L);
        DemoDO demo = new DemoDO();

        handler.insertFill(metaOf(demo));

        assertThat(demo.getCreateTime()).isNotNull();
        assertThat(demo.getUpdateTime()).isEqualTo(demo.getCreateTime());
        assertThat(demo.getCreator()).isEqualTo("1");
        assertThat(demo.getUpdater()).isEqualTo("1");
        assertThat(demo.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("update 时只填充更新时间与更新者，不碰创建信息")
    void updateFill_shouldOnlyTouchUpdateFields() {
        DefaultDBFieldHandler handler = new DefaultDBFieldHandler(() -> 2L);
        DemoDO demo = new DemoDO();

        handler.updateFill(metaOf(demo));

        assertThat(demo.getUpdateTime()).isNotNull();
        assertThat(demo.getUpdater()).isEqualTo("2");
        assertThat(demo.getCreateTime()).isNull();
        assertThat(demo.getCreator()).isNull();
    }

    @Test
    @DisplayName("已有值的字段不应被覆盖，便于数据迁移时保留原始审计信息")
    void insertFill_shouldNotOverwriteExistingValues() {
        DefaultDBFieldHandler handler = new DefaultDBFieldHandler(() -> 1L);
        LocalDateTime original = LocalDateTime.of(2020, 1, 1, 0, 0);
        DemoDO demo = new DemoDO();
        demo.setCreator("9");
        demo.setCreateTime(original);

        handler.insertFill(metaOf(demo));

        assertThat(demo.getCreator()).isEqualTo("9");
        assertThat(demo.getCreateTime()).isEqualTo(original);
    }

    @Test
    @DisplayName("拿不到当前登录用户时不应报错，审计字段留空")
    void insertFill_shouldTolerateMissingLoginUser() {
        DefaultDBFieldHandler handler = new DefaultDBFieldHandler(() -> null);
        DemoDO demo = new DemoDO();

        handler.insertFill(metaOf(demo));

        assertThat(demo.getCreateTime()).isNotNull();
        assertThat(demo.getCreator()).isNull();
        assertThat(demo.getDeleted()).isFalse();
    }
}

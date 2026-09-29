package com.ccloud.framework.mybatis.core.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ccloud.framework.mybatis.core.dataobject.BaseDO;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 审计字段自动填充。
 *
 * <p>当前登录用户通过 {@link Supplier} 注入而非直接依赖 security starter：
 * 持久层不应该反向依赖安全层，否则任何用到 MyBatis 的模块都被迫拉入安全依赖。
 */
@RequiredArgsConstructor
public class DefaultDBFieldHandler implements MetaObjectHandler {

    /**
     * 当前登录用户编号提供者。无登录态时返回 null。
     */
    private final Supplier<Long> loginUserIdSupplier;

    @Override
    public void insertFill(MetaObject metaObject) {
        if (!(metaObject.getOriginalObject() instanceof BaseDO baseDO)) {
            return;
        }
        LocalDateTime current = LocalDateTime.now();
        // 已有值不覆盖：数据迁移、数据修复等场景需要保留原始审计信息
        if (Objects.isNull(baseDO.getCreateTime())) {
            baseDO.setCreateTime(current);
        }
        if (Objects.isNull(baseDO.getUpdateTime())) {
            baseDO.setUpdateTime(baseDO.getCreateTime());
        }
        Long userId = currentUserId();
        if (Objects.nonNull(userId) && Objects.isNull(baseDO.getCreator())) {
            baseDO.setCreator(userId.toString());
        }
        if (Objects.nonNull(userId) && Objects.isNull(baseDO.getUpdater())) {
            baseDO.setUpdater(userId.toString());
        }
        if (Objects.isNull(baseDO.getDeleted())) {
            baseDO.setDeleted(false);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间必须刷新，即使调用方显式设过——它代表"最后一次修改发生在何时"
        Object modifyTime = getFieldValByName("updateTime", metaObject);
        if (Objects.isNull(modifyTime)) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
        Object modifier = getFieldValByName("updater", metaObject);
        Long userId = currentUserId();
        if (Objects.nonNull(userId) && Objects.isNull(modifier)) {
            setFieldValByName("updater", userId.toString(), metaObject);
        }
    }

    private Long currentUserId() {
        try {
            return loginUserIdSupplier == null ? null : loginUserIdSupplier.get();
        } catch (Exception e) {
            // 取不到登录用户不应阻断写库，例如定时任务、初始化脚本
            return null;
        }
    }
}

package com.ccloud.framework.mybatis.core.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据对象基类。所有 DO 都继承它，保证审计字段与逻辑删除的一致性。
 */
@Data
public abstract class BaseDO implements Serializable {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者。存的是用户编号的字符串形式，之所以不用 Long：
     * 系统任务、定时任务等场景没有真实用户，需要能写入标识字符串。
     */
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    /**
     * 逻辑删除标记：0 表示未删除，删除时写入记录自身的编号。不对外暴露，避免前端误用。
     *
     * <p>各表的唯一索引都带 deleted 列（如 (username, tenant_id, deleted)）。若删除时统一写 1，
     * 同一个值第二次被删除就会与第一次删除的记录冲突；写入各自的编号后，已删除的记录互不冲突，
     * 未删除的记录 deleted 都为 0，仍受唯一约束。delval 按原样拼入 SQL（数值字段不加引号），即 SET deleted = id
     */
    @TableLogic(value = "0", delval = "id")
    @JsonIgnore
    private Long deleted;
}

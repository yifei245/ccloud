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
     * 逻辑删除标记。不对外暴露，避免前端误用。
     */
    @TableLogic
    @JsonIgnore
    private Boolean deleted;
}

package com.ccloud.module.member.api.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员用户。不含密码等敏感字段
 */
@Data
public class MemberUserRespDTO implements Serializable {

    private Long id;
    private String nickname;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
    private String avatar;
    private String mobile;
    private LocalDateTime createTime;

    // ========== 等级与积分 ==========

    private Integer point;
    private Integer experience;
    private Long levelId;
    /**
     * 推广员编号，分销模块使用
     */
    private Long groupId;
}

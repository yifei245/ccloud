package com.ccloud.module.member.api.level.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员等级
 */
@Data
public class MemberLevelRespDTO implements Serializable {

    private Long id;
    private String name;
    /**
     * 等级序号，数值越大等级越高
     */
    private Integer level;
    /**
     * 升级所需经验
     */
    private Integer experience;
    /**
     * 享受折扣百分比，100 表示不打折
     */
    private Integer discountPercent;
    /**
     * 状态，见 CommonStatusEnum
     */
    private Integer status;
}

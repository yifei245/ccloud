package com.ccloud.module.member.api.address.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员收件地址
 */
@Data
public class MemberAddressRespDTO implements Serializable {

    private Long id;
    private Long userId;
    private String name;
    private String mobile;
    private Long areaId;
    private String detailAddress;
    private Boolean defaultStatus;
}

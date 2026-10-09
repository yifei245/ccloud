package com.ccloud.module.member.dal.dataobject.address;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccloud.framework.mybatis.core.dataobject.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 会员收件地址。每个会员至多一个默认地址
 */
@TableName("member_address")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class MemberAddressDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    /**
     * 收件人名称
     */
    private String name;
    private String mobile;
    /**
     * 地区编号，由前端维护区划数据，后端不校验、不转名称
     */
    private Long areaId;
    private String detailAddress;
    private Boolean defaultStatus;
}

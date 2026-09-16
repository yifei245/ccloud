package com.ccloud.module.system.controller.admin.tenant.vo.tenant;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 租户 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TenantRespVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("租户编号")
    private Long id;

    @Schema(description = "租户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "ccloud")
    @ExcelProperty("租户名")
    private String name;

    @Schema(description = "联系人的用户编号", example = "1")
    private Long contactUserId;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("联系人")
    private String contactName;

    @Schema(description = "联系手机", example = "13800138000")
    @ExcelProperty("联系手机")
    private String contactMobile;

    @Schema(description = "状态：0 开启 1 关闭", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(CommonStatusEnum.DICT_TYPE)
    private Integer status;

    @Schema(description = "绑定域名", example = "tenant.ccloud.local")
    @ExcelProperty("绑定域名")
    private String website;

    @Schema(description = "租户套餐编号，0 为系统租户", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long packageId;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "账号数量上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("账号数量上限")
    private Integer accountCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}

package com.ccloud.module.system.controller.admin.user.vo.user;

import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.common.enums.CommonStatusEnum;
import com.ccloud.framework.common.validation.InEnum;
import com.ccloud.framework.common.validation.Mobile;
import com.ccloud.framework.excel.core.annotations.DictFormat;
import com.ccloud.framework.excel.core.convert.DictConvert;
import com.ccloud.module.system.enums.DictTypeConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户导入 Excel 的一行，同时作为导入模板
 */
@Data
@Accessors(chain = true)
public class UserImportExcelVO {

    @ExcelProperty("登录名称")
    @NotBlank(message = "登录名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "登录名称由 4-30 位数字或字母组成")
    private String username;

    @ExcelProperty("用户名称")
    @Size(max = 30, message = "用户名称长度不能超过 30 个字符")
    private String nickname;

    @ExcelProperty("部门编号")
    private Long deptId;

    @ExcelProperty("用户邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ExcelProperty("手机号码")
    @Mobile
    private String mobile;

    @ExcelProperty(value = "用户性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.USER_SEX)
    private Integer sex;

    @ExcelProperty(value = "账号状态", converter = DictConvert.class)
    @DictFormat(CommonStatusEnum.DICT_TYPE)
    @InEnum(value = CommonStatusEnum.class, message = "账号状态必须是 {value}")
    private Integer status;
}

package com.ccloud.framework.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数基类。所有分页查询 VO 继承它。
 */
@Data
@Schema(description = "分页参数")
public class PageParam implements Serializable {

    public static final Integer PAGE_NO = 1;
    public static final Integer PAGE_SIZE = 10;

    /**
     * 传入该值表示不分页，一次返回全部数据。
     * 仅用于导出等确实需要全量的场景，普通查询禁止使用。
     */
    public static final Integer PAGE_SIZE_NONE = -1;

    @Schema(description = "页码，从 1 开始", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

    @Schema(description = "每页条数，最大 100", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    private Integer pageSize = PAGE_SIZE;
}

package com.ccloud.framework.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 支持动态排序的分页参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支持排序的分页参数")
public class SortablePageParam extends PageParam {

    @Schema(description = "排序字段，可多个，按顺序生效")
    private List<SortingField> sortingFields;
}

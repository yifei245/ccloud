package com.ccloud.framework.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 排序字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "排序字段")
public class SortingField implements Serializable {

    public static final String ORDER_ASC = "asc";
    public static final String ORDER_DESC = "desc";

    @Schema(description = "字段名，需与数据库列名对应", example = "createTime")
    private String field;

    @Schema(description = "排序方向：asc 或 desc", example = "desc")
    private String order;
}

package com.ccloud.framework.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Schema(description = "当前页数据")
    private List<T> list;

    @Schema(description = "总条数", example = "100")
    private Long total;

    public PageResult(Long total) {
        this(Collections.emptyList(), total);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L);
    }

    public static <T> PageResult<T> empty(Long total) {
        return new PageResult<>(Collections.emptyList(), total);
    }
}

package com.ccloud.framework.mybatis.core.util;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.SortingField;

import java.util.List;
import java.util.Objects;

/**
 * MyBatis Plus 工具
 */
public class MyBatisUtils {

    private static final String MYSQL_ESCAPE_CHARACTER = "`";

    public static <T> Page<T> buildPage(PageParam pageParam) {
        return buildPage(pageParam, null);
    }

    public static <T> Page<T> buildPage(PageParam pageParam, List<SortingField> sortingFields) {
        // pageSize 为 -1 表示不分页：构造一个足够大的页，同时关闭 count 查询以省一次扫表
        if (Objects.equals(pageParam.getPageSize(), PageParam.PAGE_SIZE_NONE)) {
            Page<T> page = new Page<>(1, Integer.MAX_VALUE, false);
            addOrder(page, sortingFields);
            return page;
        }
        Page<T> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        addOrder(page, sortingFields);
        return page;
    }

    private static <T> void addOrder(Page<T> page, List<SortingField> sortingFields) {
        if (CollUtil.isEmpty(sortingFields)) {
            return;
        }
        page.addOrder(sortingFields.stream().map(field -> SortingField.ORDER_ASC.equals(field.getOrder())
                ? OrderItem.asc(field.getField()) : OrderItem.desc(field.getField())).toList());
    }

    /**
     * 给字段名加上反引号，避免与 MySQL 关键字冲突
     */
    public static String escapeColumn(String column) {
        return MYSQL_ESCAPE_CHARACTER + column + MYSQL_ESCAPE_CHARACTER;
    }
}

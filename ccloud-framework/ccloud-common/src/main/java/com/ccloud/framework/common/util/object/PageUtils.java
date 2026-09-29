package com.ccloud.framework.common.util.object;

import com.ccloud.framework.common.pojo.PageParam;

/**
 * 分页计算工具
 */
public class PageUtils {

    /**
     * 计算 SQL 的 offset
     */
    public static int getStart(PageParam pageParam) {
        return (pageParam.getPageNo() - 1) * pageParam.getPageSize();
    }

    /**
     * 计算总页数
     */
    public static int getTotalPage(long total, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }
}

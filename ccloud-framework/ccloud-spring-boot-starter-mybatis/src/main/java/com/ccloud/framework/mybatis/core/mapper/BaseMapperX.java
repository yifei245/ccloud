package com.ccloud.framework.mybatis.core.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.ccloud.framework.common.pojo.PageParam;
import com.ccloud.framework.common.pojo.PageResult;
import com.ccloud.framework.common.pojo.SortablePageParam;
import com.ccloud.framework.mybatis.core.util.MyBatisUtils;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 增强版 Mapper。在 MyBatis Plus 的基础上补上项目里高频使用的几类查询，
 * 目的是让 Service 层不必反复手写 QueryWrapper 样板。
 */
public interface BaseMapperX<T> extends MPJBaseMapper<T> {

    default PageResult<T> selectPage(SortablePageParam pageParam, Wrapper<T> queryWrapper) {
        return selectPage(pageParam, pageParam.getSortingFields(), queryWrapper);
    }

    default PageResult<T> selectPage(PageParam pageParam, Wrapper<T> queryWrapper) {
        return selectPage(pageParam, null, queryWrapper);
    }

    default PageResult<T> selectPage(PageParam pageParam,
                                     List<com.ccloud.framework.common.pojo.SortingField> sortingFields,
                                     Wrapper<T> queryWrapper) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> mpPage =
                MyBatisUtils.buildPage(pageParam, sortingFields);
        selectPage(mpPage, queryWrapper);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    default T selectOne(String field, Object value) {
        return selectOne(new QueryWrapper<T>().eq(field, value));
    }

    default T selectOne(SFunction<T, ?> field, Object value) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default T selectOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default Long selectCount() {
        return selectCount(new QueryWrapper<>());
    }

    default Long selectCount(String field, Object value) {
        return selectCount(new QueryWrapper<T>().eq(field, value));
    }

    default Long selectCount(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList() {
        return selectList(new QueryWrapper<>());
    }

    default List<T> selectList(String field, Object value) {
        return selectList(new QueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList(SFunction<T, ?> field, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<T>().in(field, values));
    }

    /**
     * 批量插入。走 MP 的 Db 工具，底层是 JDBC batch，
     * 比循环单条 insert 快一个数量级。
     */
    default void insertBatch(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        Db.saveBatch(entities);
    }

    default void insertBatch(Collection<T> entities, int size) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        Db.saveBatch(entities, size);
    }

    default void updateBatch(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        Db.updateBatchById(entities);
    }

    default int delete(String field, Object value) {
        return delete(new QueryWrapper<T>().eq(field, value));
    }

    default int delete(SFunction<T, ?> field, Object value) {
        return delete(new LambdaQueryWrapper<T>().eq(field, value));
    }
}

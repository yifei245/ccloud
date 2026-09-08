package com.ccloud.framework.common.util.object;

import com.ccloud.framework.common.pojo.PageResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bean 转换工具。DO / VO / DTO 之间的转换统一走这里，避免每处手写属性拷贝。
 */
public class BeanUtils {

    public static <T> T toBean(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return cn.hutool.core.bean.BeanUtil.toBean(source, targetClass);
    }

    public static <T> T toBean(Object source, Class<T> targetClass, Consumer<T> peek) {
        T target = toBean(source, targetClass);
        if (target != null && peek != null) {
            peek.accept(target);
        }
        return target;
    }

    public static <S, T> List<T> toBean(List<S> source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return source.stream().map(s -> toBean(s, targetClass)).toList();
    }

    public static <S, T> List<T> toBean(List<S> source, Class<T> targetClass, Consumer<T> peek) {
        List<T> list = toBean(source, targetClass);
        if (list != null && peek != null) {
            list.forEach(peek);
        }
        return list;
    }

    public static <S, T> PageResult<T> toBean(PageResult<S> source, Class<T> targetClass) {
        return toBean(source, targetClass, null);
    }

    public static <S, T> PageResult<T> toBean(PageResult<S> source, Class<T> targetClass, Consumer<T> peek) {
        if (source == null) {
            return null;
        }
        List<T> list = toBean(source.getList(), targetClass);
        if (list != null && peek != null) {
            list.forEach(peek);
        }
        return new PageResult<>(list, source.getTotal());
    }

    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        cn.hutool.core.bean.BeanUtil.copyProperties(source, target, false);
    }
}

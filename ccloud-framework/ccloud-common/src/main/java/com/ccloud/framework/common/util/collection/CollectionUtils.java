package com.ccloud.framework.common.util.collection;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 集合工具。全部方法对 null 入参安全，返回空集合而非 null。
 */
public class CollectionUtils {

    public static boolean isAnyEmpty(Collection<?>... collections) {
        return Arrays.stream(collections).anyMatch(CollUtil::isEmpty);
    }

    public static <T> boolean containsAny(Collection<T> source, Collection<T> candidates) {
        return CollUtil.containsAny(source, candidates);
    }

    public static <T> List<T> filterList(Collection<T> from, Predicate<T> predicate) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 按 keyMapper 去重，保留首次出现的元素
     */
    public static <T, R> List<T> distinct(Collection<T> from, Function<T, R> keyMapper) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream()
                .collect(Collectors.toMap(keyMapper, t -> t, (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
    }

    public static <T, U> List<U> convertList(Collection<T> from, Function<T, U> func) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().map(func).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T, U> List<U> convertList(Collection<T> from, Function<T, U> func, Predicate<T> filter) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().filter(filter).map(func).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 一对多展开后再拍平，例如把每个用户的角色 ID 列表汇总成一个大列表
     */
    public static <T, U> List<U> convertListByFlatMap(Collection<T> from,
                                                      Function<T, ? extends java.util.stream.Stream<? extends U>> func) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().flatMap(func).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T, U> Set<U> convertSet(Collection<T> from, Function<T, U> func) {
        if (CollUtil.isEmpty(from)) {
            return new HashSet<>();
        }
        return from.stream().map(func).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <T, K> Map<K, T> convertMap(Collection<T> from, Function<T, K> keyFunc) {
        if (CollUtil.isEmpty(from)) {
            return new HashMap<>();
        }
        return convertMap(from, keyFunc, Function.identity());
    }

    public static <T, K, V> Map<K, V> convertMap(Collection<T> from, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(from)) {
            return new HashMap<>();
        }
        // 重复键保留后者，与"后写入覆盖"的直觉一致
        return from.stream().collect(Collectors.toMap(keyFunc, valueFunc, (a, b) -> b));
    }

    public static <T, K, V> Map<K, V> convertMap(Collection<T> from, Function<T, K> keyFunc,
                                                 Function<T, V> valueFunc, Supplier<? extends Map<K, V>> supplier) {
        if (CollUtil.isEmpty(from)) {
            return supplier.get();
        }
        return from.stream().collect(Collectors.toMap(keyFunc, valueFunc, (a, b) -> b, supplier));
    }

    public static <T, K> Map<K, List<T>> convertMultiMap(Collection<T> from, Function<T, K> keyFunc) {
        if (CollUtil.isEmpty(from)) {
            return new HashMap<>();
        }
        return from.stream().collect(Collectors.groupingBy(keyFunc, Collectors.toList()));
    }

    public static <T, K, V> Map<K, List<V>> convertMultiMap(Collection<T> from, Function<T, K> keyFunc,
                                                            Function<T, V> valueFunc) {
        if (CollUtil.isEmpty(from)) {
            return new HashMap<>();
        }
        return from.stream().collect(Collectors.groupingBy(keyFunc, Collectors.mapping(valueFunc, Collectors.toList())));
    }

    public static <T> T findFirst(Collection<T> from, Predicate<T> predicate) {
        if (CollUtil.isEmpty(from)) {
            return null;
        }
        return from.stream().filter(predicate).findFirst().orElse(null);
    }

    public static <T, U> U findFirst(Collection<T> from, Predicate<T> predicate, Function<T, U> func) {
        if (CollUtil.isEmpty(from)) {
            return null;
        }
        return from.stream().filter(predicate).findFirst().map(func).orElse(null);
    }

    public static <T> boolean anyMatch(Collection<T> from, Predicate<T> predicate) {
        return CollUtil.isNotEmpty(from) && from.stream().anyMatch(predicate);
    }

    public static <T> T getFirst(List<T> from) {
        return CollUtil.isNotEmpty(from) ? from.get(0) : null;
    }

    public static <T> void addIfNotNull(Collection<T> coll, T item) {
        if (item != null) {
            coll.add(item);
        }
    }

    public static <T> List<T> singletonList(T obj) {
        return obj == null ? new ArrayList<>() : Collections.singletonList(obj);
    }

    /**
     * 对比新旧两份数据，切分成"新增 / 修改 / 删除"三组。
     * 典型用途：编辑表单里的子表，一次提交同时包含三类变更。
     *
     * @param oldList    数据库中的旧数据
     * @param newList    前端提交的新数据
     * @param sameFunc   判断两条记录是否为同一条（通常比较主键）
     * @return 下标 0 = 新增，1 = 修改，2 = 删除
     */
    public static <T> List<List<T>> diffList(Collection<T> oldList, Collection<T> newList,
                                             BiFunction<T, T, Boolean> sameFunc) {
        List<T> createList = new LinkedList<>(newList);
        List<T> updateList = new ArrayList<>();
        List<T> deleteList = new ArrayList<>();

        for (T oldObj : oldList) {
            T foundObj = null;
            for (Iterator<T> iterator = createList.iterator(); iterator.hasNext(); ) {
                T newObj = iterator.next();
                if (!sameFunc.apply(oldObj, newObj)) {
                    continue;
                }
                iterator.remove();
                foundObj = newObj;
                break;
            }
            if (foundObj != null) {
                updateList.add(foundObj);
            } else {
                deleteList.add(oldObj);
            }
        }
        return List.of(createList, updateList, deleteList);
    }

    public static <K, V> Map<K, V> emptyIfNull(Map<K, V> map) {
        return map == null ? ImmutableMap.of() : map;
    }
}

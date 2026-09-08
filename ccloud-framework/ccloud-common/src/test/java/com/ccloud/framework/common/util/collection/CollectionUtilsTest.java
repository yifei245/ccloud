package com.ccloud.framework.common.util.collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionUtilsTest {

    record User(Long id, String name, Long deptId) {}

    private final List<User> users = List.of(
            new User(1L, "张三", 100L),
            new User(2L, "李四", 100L),
            new User(3L, "王五", 200L));

    @Test
    @DisplayName("convertList 应按字段抽取为列表并保持顺序")
    void convertList_shouldExtractFieldPreservingOrder() {
        assertThat(CollectionUtils.convertList(users, User::name))
                .containsExactly("张三", "李四", "王五");
    }

    @Test
    @DisplayName("convertList 传入 null 集合应返回空列表而非 NPE")
    void convertList_shouldReturnEmptyForNullInput() {
        assertThat(CollectionUtils.convertList((List<User>) null, User::name)).isEmpty();
    }

    @Test
    @DisplayName("convertSet 应去重")
    void convertSet_shouldDeduplicate() {
        assertThat(CollectionUtils.convertSet(users, User::deptId)).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("convertMap 应以指定字段为键")
    void convertMap_shouldKeyByField() {
        Map<Long, User> map = CollectionUtils.convertMap(users, User::id);
        assertThat(map).hasSize(3);
        assertThat(map.get(2L).name()).isEqualTo("李四");
    }

    @Test
    @DisplayName("convertMultiMap 应把同键元素聚合为列表")
    void convertMultiMap_shouldGroupByKey() {
        Map<Long, List<User>> map = CollectionUtils.convertMultiMap(users, User::deptId);
        assertThat(map.get(100L)).hasSize(2);
        assertThat(map.get(200L)).hasSize(1);
    }

    @Test
    @DisplayName("filterList 应按条件过滤")
    void filterList_shouldFilterByPredicate() {
        assertThat(CollectionUtils.filterList(users, u -> u.deptId().equals(100L))).hasSize(2);
    }

    @Test
    @DisplayName("findFirst 命中返回元素，未命中返回 null")
    void findFirst_shouldReturnMatchOrNull() {
        assertThat(CollectionUtils.findFirst(users, u -> u.id().equals(3L))).isNotNull()
                .extracting(User::name).isEqualTo("王五");
        assertThat(CollectionUtils.findFirst(users, u -> u.id().equals(99L))).isNull();
    }

    @Test
    @DisplayName("anyMatch 应正确判断是否存在匹配元素")
    void anyMatch_shouldDetectPresence() {
        assertThat(CollectionUtils.anyMatch(users, u -> u.deptId().equals(200L))).isTrue();
        assertThat(CollectionUtils.anyMatch(users, u -> u.deptId().equals(999L))).isFalse();
    }

    @Test
    @DisplayName("diffList 应区分出新增、修改与删除三组")
    void diffList_shouldSplitIntoCreateUpdateDelete() {
        List<User> oldList = List.of(new User(1L, "张三", 100L), new User(2L, "李四", 100L));
        List<User> newList = List.of(new User(1L, "张三改", 100L), new User(null, "赵六", 300L));
        List<List<User>> result = CollectionUtils.diffList(oldList, newList,
                (o, n) -> o.id().equals(n.id()));
        assertThat(result.get(0)).extracting(User::name).containsExactly("赵六");   // 新增
        assertThat(result.get(1)).extracting(User::name).containsExactly("张三改"); // 修改
        assertThat(result.get(2)).extracting(User::name).containsExactly("李四");   // 删除
    }
}

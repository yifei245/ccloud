package com.ccloud.framework.operatelog.core.util;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 日志脱敏：字段名命中敏感词（不区分大小写，含嵌套对象与数组）时值替换为 ******
 */
public final class DesensitizeUtils {

    public static final String MASK = "******";

    private static final Pattern SENSITIVE_KEY =
            Pattern.compile("(?i).*(password|passwd|pwd|secret|token|credential|privatekey).*");

    private DesensitizeUtils() {
    }

    /**
     * @return 脱敏后的 JSON 树；对象无法序列化时返回其 toString
     */
    public static Object desensitize(Object value) {
        return desensitize(value, Collections.emptySet());
    }

    /**
     * @param extraKeys 调用方额外指定的敏感字段名，精确匹配、不区分大小写
     */
    public static Object desensitize(Object value, Collection<String> extraKeys) {
        if (value == null) {
            return null;
        }
        Set<String> extra = extraKeys.stream().map(k -> k.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        JsonNode node = value instanceof JsonNode json ? json.deepCopy() : JsonUtils.getObjectMapper().valueToTree(value);
        mask(node, extra);
        return node;
    }

    private static void mask(JsonNode node, Set<String> extra) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_KEY.matcher(field.getKey()).matches() || extra.contains(field.getKey().toLowerCase(Locale.ROOT))) {
                    ((ObjectNode) node).put(field.getKey(), MASK);
                } else {
                    mask(field.getValue(), extra);
                }
            }
        } else if (node.isArray()) {
            node.forEach(child -> mask(child, extra));
        }
    }
}

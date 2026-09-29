package com.ccloud.framework.operatelog.core.util;

import com.ccloud.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

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
        if (value == null) {
            return null;
        }
        JsonNode node = JsonUtils.getObjectMapper().valueToTree(value);
        mask(node);
        return node;
    }

    private static void mask(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_KEY.matcher(field.getKey()).matches()) {
                    ((ObjectNode) node).put(field.getKey(), MASK);
                } else {
                    mask(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(DesensitizeUtils::mask);
        }
    }
}

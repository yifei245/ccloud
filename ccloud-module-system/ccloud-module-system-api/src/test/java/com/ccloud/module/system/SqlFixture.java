package com.ccloud.module.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取仓库中的初始化 SQL，供一致性测试比对。
 *
 * <p>代码里的枚举、错误码与 SQL 里的字典、文案是两份独立维护的数据，
 * 一旦漂移，界面上会出现下拉框没有选项、错误提示显示成 code 这类问题，
 * 且编译与单测都发现不了。这里把两份数据放在一起直接比对。
 */
public final class SqlFixture {

    private static final Path DML_DIR = locateDmlDir();

    private SqlFixture() {
    }

    private static Path locateDmlDir() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParent()) {
            Path candidate = dir.resolve("sql/mysql/dml");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("找不到 sql/mysql/dml 目录，一致性测试需要在仓库内运行");
    }

    public static String read(String fileName) {
        try {
            return Files.readString(DML_DIR.resolve(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 " + fileName + " 失败", e);
        }
    }

    /**
     * 解析 04-dict.sql 中某个字典类型的全部取值
     */
    public static Set<String> dictValues(String dictType) {
        // 行格式：(id, sort, 'label', 'value', 'dict_type', ...)
        Pattern pattern = Pattern.compile("\\(\\d+, \\d+, '[^']*', '([^']*)', '" + Pattern.quote(dictType) + "',");
        Matcher matcher = pattern.matcher(read("04-dict.sql"));
        Set<String> values = new TreeSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    /**
     * 解析 06-i18n.sql 中指定语言的全部文案编码
     */
    public static Set<String> i18nCodes(String locale) {
        Pattern pattern = Pattern.compile("\\(\\d+, '" + Pattern.quote(locale) + "', '([^']+)', '[^']*'");
        Matcher matcher = pattern.matcher(read("06-i18n.sql"));
        Set<String> codes = new HashSet<>();
        while (matcher.find()) {
            codes.add(matcher.group(1));
        }
        return codes;
    }

    public static boolean contains(String fileName, String literal) {
        return read(fileName).contains(literal);
    }
}

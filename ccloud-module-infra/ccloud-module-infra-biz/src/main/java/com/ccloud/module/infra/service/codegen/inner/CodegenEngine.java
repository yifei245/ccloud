package com.ccloud.module.infra.service.codegen.inner;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 代码生成引擎：按表与字段定义渲染 Velocity 模板。只做渲染，不访问数据库，便于单测
 */
@Component
public class CodegenEngine {

    private static final String BASE_PACKAGE = "com.ccloud.module";

    private final VelocityEngine velocity;

    public CodegenEngine() {
        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        properties.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        properties.setProperty(RuntimeConstants.INPUT_ENCODING, StandardCharsets.UTF_8.name());
        properties.setProperty(RuntimeConstants.SPACE_GOBBLING, "lines");
        // 模板里引用了不存在的变量直接报错，而不是悄悄输出 $xxx 生成无法编译的代码
        properties.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "true");
        velocity = new VelocityEngine(properties);
        velocity.init();
    }

    /**
     * @return 文件路径（相对仓库根目录）→ 内容，按生成顺序
     */
    public Map<String, String> execute(CodegenTableDO table, List<CodegenColumnDO> columns) {
        Map<String, Object> context = buildContext(table, columns);
        Map<String, String> templates = templatePaths(table, context);
        Map<String, String> result = new LinkedHashMap<>();
        templates.forEach((template, path) -> {
            StringWriter writer = new StringWriter();
            velocity.getTemplate(template, StandardCharsets.UTF_8.name()).merge(new VelocityContext(new HashMap<>(context)), writer);
            result.put(path, writer.toString());
        });
        return result;
    }

    private Map<String, Object> buildContext(CodegenTableDO table, List<CodegenColumnDO> columns) {
        CodegenSceneEnum scene = CodegenSceneEnum.valueOf(table.getScene());
        CodegenValidator.validate(table, columns);
        boolean tree = CodegenTemplateTypeEnum.TREE.getType().equals(table.getTemplateType());
        List<CodegenColumnDO> sorted = columns.stream()
                .sorted(Comparator.comparing(CodegenColumnDO::getOrdinalPosition)).toList();
        Map<Long, CodegenColumnView> views = new LinkedHashMap<>();
        sorted.forEach(c -> views.put(c.getId(), new CodegenColumnView(c, tree && c.getId().equals(table.getTreeParentColumnId()))));
        Function<Predicate<CodegenColumnDO>, List<CodegenColumnView>> pick =
                filter -> sorted.stream().filter(filter).map(c -> views.get(c.getId())).toList();

        String className = table.getClassName();
        String prefix = scene.getPrefix();
        String module = table.getModuleName();
        String kebab = StrUtil.toSymbolCase(className, '-');
        CodegenColumnView pk = pick.apply(c -> Boolean.TRUE.equals(c.getPrimaryKey())).get(0);

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("basePackage", BASE_PACKAGE);
        ctx.put("table", table);
        ctx.put("module", module);
        ctx.put("moduleUpper", StrUtil.upperFirst(module));
        ctx.put("business", table.getBusinessName());
        ctx.put("className", className);
        ctx.put("classNameVar", StrUtil.lowerFirst(className));
        ctx.put("classComment", CodegenColumnView.escapeJava(table.getClassComment()));
        ctx.put("classJavadoc", CodegenColumnView.escapeJavadoc(table.getClassComment()));
        ctx.put("sqlClassComment", escapeSql(table.getClassComment()));
        ctx.put("authorJavadoc", CodegenColumnView.escapeJavadoc(table.getAuthor()));
        ctx.put("kebab", kebab);
        ctx.put("permissionPrefix", module + ":" + kebab);
        ctx.put("isAdmin", scene == CodegenSceneEnum.ADMIN);
        ctx.put("isTree", tree);
        ctx.put("sceneEnum", scene.getBasePackage());
        ctx.put("sceneLabel", scene.getLabel());
        ctx.put("scenePrefix", prefix);
        ctx.put("urlPrefix", scene.getUrlPrefix());
        ctx.put("voPackage", BASE_PACKAGE + "." + module + ".controller." + scene.getBasePackage() + "."
                + table.getBusinessName() + ".vo");
        ctx.put("saveReqVO", prefix + className + "SaveReqVO");
        ctx.put("pageReqVO", prefix + className + "PageReqVO");
        ctx.put("listReqVO", prefix + className + "ListReqVO");
        ctx.put("queryReqVO", prefix + className + (tree ? "ListReqVO" : "PageReqVO"));
        ctx.put("respVO", prefix + className + "RespVO");
        ctx.put("baseDOClass", sorted.stream().anyMatch(c -> "tenant_id".equals(c.getColumnName())) ? "TenantBaseDO" : "BaseDO");
        ctx.put("primaryColumn", pk);
        ctx.put("pkType", pk.getJavaType());
        ctx.put("pkUpper", pk.getJavaFieldUpper());
        ctx.put("doColumns", pick.apply(c -> !CodegenBuilder.BASE_COLUMNS.contains(c.getColumnName())));
        ctx.put("saveColumns", pick.apply(c -> !Boolean.TRUE.equals(c.getPrimaryKey())
                && (Boolean.TRUE.equals(c.getCreateOperation()) || Boolean.TRUE.equals(c.getUpdateOperation()))));
        ctx.put("listColumns", pick.apply(c -> Boolean.TRUE.equals(c.getListOperation())));
        ctx.put("resultColumns", pick.apply(c -> Boolean.TRUE.equals(c.getListOperationResult())));
        ctx.put("javaTypeImports", javaTypeImports(sorted));
        if (tree) {
            CodegenColumnView parent = views.get(table.getTreeParentColumnId());
            ctx.put("treeParentColumn", parent);
            ctx.put("parentType", parent.getJavaType());
            ctx.put("parentUpper", parent.getJavaFieldUpper());
            ctx.put("treeNameColumn", views.get(table.getTreeNameColumnId()));
        }
        // 错误码：1-099 为生成代码的预留段，合并进模块时由使用者重新编号
        String errorPrefix = StrUtil.toUnderlineCase(className).toUpperCase(Locale.ROOT);
        long tableNo = table.getId() % 1000;
        List<Map<String, String>> errors = new ArrayList<>();
        String comment = table.getClassComment();
        errors.add(error(errorPrefix + "_NOT_EXISTS", tableNo, 0, comment + "不存在", className + " does not exist"));
        if (tree) {
            errors.add(error(errorPrefix + "_EXISTS_CHILDREN", tableNo, 1, "存在子" + comment + "，无法删除",
                    className + " has children and cannot be deleted"));
            errors.add(error(errorPrefix + "_PARENT_NOT_EXISTS", tableNo, 2, "父级" + comment + "不存在",
                    "The parent " + className + " does not exist"));
            errors.add(error(errorPrefix + "_PARENT_ERROR", tableNo, 3, "不能设置自己为父" + comment,
                    className + " cannot be its own parent"));
            errors.add(error(errorPrefix + "_PARENT_IS_CHILD", tableNo, 4, "不能设置自己的子" + comment + "为父" + comment,
                    "A child " + className + " cannot be the parent"));
        }
        ctx.put("errorCodePrefix", errorPrefix);
        ctx.put("errorCodes", errors);
        ctx.put("parentMenuId", ObjectUtil.defaultIfNull(table.getParentMenuId(), 0L));
        return ctx;
    }

    private static Map<String, String> error(String name, long tableNo, int index, String zh, String en) {
        return Map.of("name", name,
                "javaCode", String.format("1_099_%03d_%03d", tableNo, index),
                "code", String.format("1099%03d%03d", tableNo, index),
                "zh", CodegenColumnView.escapeJava(zh), "sqlZh", escapeSql(zh), "sqlEn", escapeSql(en));
    }

    /**
     * 用于 MySQL 单引号字符串：反斜杠在 MySQL 中是转义符，与单引号一起加倍；换行改为空格，避免破坏行注释
     */
    private static String escapeSql(String text) {
        return text.replace("\\", "\\\\").replace("'", "''").replace("\r", " ").replace("\n", " ");
    }

    private static Set<String> javaTypeImports(List<CodegenColumnDO> columns) {
        Map<String, String> imports = Map.of("BigDecimal", "java.math.BigDecimal", "LocalDateTime", "java.time.LocalDateTime",
                "LocalDate", "java.time.LocalDate", "LocalTime", "java.time.LocalTime");
        return columns.stream().map(c -> imports.get(c.getJavaType())).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Map<String, String> templatePaths(CodegenTableDO table, Map<String, Object> ctx) {
        String module = table.getModuleName();
        String business = table.getBusinessName();
        String className = table.getClassName();
        String root = "ccloud-module-" + module + "/ccloud-module-" + module;
        String bizJava = root + "-biz/src/main/java/com/ccloud/module/" + module;
        String bizTest = root + "-biz/src/test/java/com/ccloud/module/" + module;
        String apiJava = root + "-api/src/main/java/com/ccloud/module/" + module;
        String controller = bizJava + "/controller/" + ctx.get("sceneEnum") + "/" + business;
        String prefix = (String) ctx.get("scenePrefix");

        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("codegen/java/dal/do.vm", bizJava + "/dal/dataobject/" + business + "/" + className + "DO.java");
        paths.put("codegen/java/dal/mapper.vm", bizJava + "/dal/mysql/" + business + "/" + className + "Mapper.java");
        paths.put("codegen/java/service/service.vm", bizJava + "/service/" + business + "/" + className + "Service.java");
        paths.put("codegen/java/service/serviceImpl.vm", bizJava + "/service/" + business + "/" + className + "ServiceImpl.java");
        paths.put("codegen/java/controller/controller.vm", controller + "/" + prefix + className + "Controller.java");
        if (Boolean.TRUE.equals(ctx.get("isTree"))) {
            paths.put("codegen/java/controller/vo/listReqVO.vm", controller + "/vo/" + ctx.get("listReqVO") + ".java");
        } else {
            paths.put("codegen/java/controller/vo/pageReqVO.vm", controller + "/vo/" + ctx.get("pageReqVO") + ".java");
        }
        paths.put("codegen/java/controller/vo/respVO.vm", controller + "/vo/" + ctx.get("respVO") + ".java");
        paths.put("codegen/java/controller/vo/saveReqVO.vm", controller + "/vo/" + ctx.get("saveReqVO") + ".java");
        paths.put("codegen/java/test/serviceImplTest.vm", bizTest + "/service/" + business + "/" + className + "ServiceImplTest.java");
        paths.put("codegen/java/enums/errorCode.vm", apiJava + "/enums/ErrorCodeConstants_" + className + ".java.txt");
        paths.put("codegen/sql/sql.vm", "sql/codegen/" + table.getTableName() + ".sql");
        return paths;
    }
}

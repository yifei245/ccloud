package com.ccloud.module.infra.service.codegen;

import com.ccloud.framework.common.util.object.BeanUtils;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenColumnSaveReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenTableSaveReqVO;
import com.ccloud.module.infra.controller.admin.codegen.vo.CodegenUpdateReqVO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.ccloud.module.infra.enums.codegen.CodegenSceneEnum;
import com.ccloud.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.ccloud.module.infra.service.codegen.inner.CodegenBuilder;
import com.ccloud.module.infra.service.codegen.inner.CodegenEngine;
import com.ccloud.module.infra.service.db.DataSourceConfigServiceImpl;
import com.ccloud.module.infra.service.db.DatabaseTableServiceImpl;
import com.ccloud.module.infra.test.BaseInfraDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 供 scripts/verify-codegen.sh 使用：把两张演示表的生成结果写到环境变量 CCLOUD_CODEGEN_OUTPUT 指定的目录。
 * 未设置该变量时跳过，不参与日常测试
 */
@EnabledIfEnvironmentVariable(named = "CCLOUD_CODEGEN_OUTPUT", matches = ".+")
@Import({CodegenServiceImpl.class, CodegenBuilder.class, CodegenEngine.class,
        DatabaseTableServiceImpl.class, DataSourceConfigServiceImpl.class})
class CodegenOutputWriterTest extends BaseInfraDbUnitTest {

    @Resource
    private CodegenServiceImpl codegenService;

    @Test
    void 生成两张演示表的代码() throws IOException {
        Path output = Path.of(System.getenv("CCLOUD_CODEGEN_OUTPUT"));
        Long studentId = codegenService.createCodegenList(0L, List.of("infra_demo_student")).get(0);
        Long categoryId = codegenService.createCodegenList(0L, List.of("infra_demo_category")).get(0);

        // 分类表：用户 App 场景的树表
        CodegenTableDO category = codegenService.getCodegenTable(categoryId);
        List<CodegenColumnDO> columns = codegenService.getCodegenColumnListByTableId(categoryId);
        CodegenUpdateReqVO reqVO = new CodegenUpdateReqVO();
        CodegenTableSaveReqVO table = BeanUtils.toBean(category, CodegenTableSaveReqVO.class);
        table.setScene(CodegenSceneEnum.APP.getScene());
        table.setTemplateType(CodegenTemplateTypeEnum.TREE.getType());
        table.setTreeParentColumnId(findId(columns, "parent_id"));
        table.setTreeNameColumnId(findId(columns, "name"));
        reqVO.setTable(table);
        reqVO.setColumns(BeanUtils.toBean(columns, CodegenColumnSaveReqVO.class));
        codegenService.updateCodegen(reqVO);

        int count = 0;
        for (Long tableId : List.of(studentId, categoryId)) {
            for (Map.Entry<String, String> file : codegenService.generationCodes(tableId).entrySet()) {
                Path target = output.resolve(file.getKey());
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
                count++;
            }
        }
        assertThat(count).isEqualTo(22);
    }

    private static Long findId(List<CodegenColumnDO> columns, String name) {
        return columns.stream().filter(c -> c.getColumnName().equals(name)).findFirst().orElseThrow().getId();
    }
}

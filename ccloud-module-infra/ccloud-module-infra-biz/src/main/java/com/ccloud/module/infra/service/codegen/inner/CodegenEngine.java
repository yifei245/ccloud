package com.ccloud.module.infra.service.codegen.inner;

import com.ccloud.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.ccloud.module.infra.dal.dataobject.codegen.CodegenTableDO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 代码生成引擎：按表与字段定义渲染模板。Task 16 实现
 */
@Component
public class CodegenEngine {

    /**
     * @return 文件路径 → 内容，按生成顺序
     */
    public Map<String, String> execute(CodegenTableDO table, List<CodegenColumnDO> columns) {
        throw new UnsupportedOperationException("Task 16");
    }
}

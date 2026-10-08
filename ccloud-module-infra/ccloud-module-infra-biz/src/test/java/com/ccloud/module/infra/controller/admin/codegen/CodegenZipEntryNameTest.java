package com.ccloud.module.infra.controller.admin.codegen;

import com.ccloud.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static com.ccloud.module.infra.enums.ErrorCodeConstants.CODEGEN_INVALID_IDENTIFIER;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodegenZipEntryNameTest {

    @Test
    void 压缩包条目须为相对路径_不能逃出根目录() {
        assertThatCode(() -> CodegenController.validateZipEntryName("sql/codegen/infra_demo_student.sql")).doesNotThrowAnyException();
        assertThatCode(() -> CodegenController.validateZipEntryName("a/../b.java")).doesNotThrowAnyException();
        for (String path : new String[]{"../a.java", "a/../../b.java", "/etc/a.java", "a\\..\\..\\b.java"}) {
            assertThatThrownBy(() -> CodegenController.validateZipEntryName(path)).as(path)
                    .isInstanceOf(ServiceException.class)
                    .extracting("code").isEqualTo(CODEGEN_INVALID_IDENTIFIER.getCode());
        }
    }
}

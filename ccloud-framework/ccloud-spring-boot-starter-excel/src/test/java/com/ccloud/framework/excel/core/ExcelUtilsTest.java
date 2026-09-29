package com.ccloud.framework.excel.core;

import cn.idev.excel.annotation.ExcelProperty;
import com.ccloud.framework.excel.core.util.ExcelUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelUtilsTest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemoVO {
        @ExcelProperty("用户名")
        private String username;

        @ExcelProperty("昵称")
        private String nickname;
    }

    @Test
    @DisplayName("导出的内容应能被重新读回，且行数与字段一致")
    void writeThenRead_shouldRoundTrip() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<DemoVO> data = List.of(new DemoVO("admin", "管理员"), new DemoVO("ccloud", "普通用户"));

        ExcelUtils.write(response, "用户.xlsx", "用户列表", DemoVO.class, data);

        byte[] bytes = response.getContentAsByteArray();
        assertThat(bytes).isNotEmpty();

        List<DemoVO> read = ExcelUtils.read(new ByteArrayInputStream(bytes), DemoVO.class);
        assertThat(read).hasSize(2);
        assertThat(read.get(0).getUsername()).isEqualTo("admin");
        assertThat(read.get(1).getNickname()).isEqualTo("普通用户");
    }

    @Test
    @DisplayName("导出应设置附件响应头，且文件名经过 URL 编码以支持中文")
    void write_shouldSetAttachmentHeaderWithEncodedFilename() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.write(response, "用户列表.xlsx", "sheet", DemoVO.class, List.of());

        assertThat(response.getContentType()).contains("application/vnd.openxmlformats");
        String disposition = response.getHeader("Content-Disposition");
        assertThat(disposition).startsWith("attachment;").contains("filename=");
        // 中文必须编码，否则部分浏览器会得到乱码文件名
        assertThat(disposition).doesNotContain("用户列表");
    }

    @Test
    @DisplayName("读取上传文件应返回全部数据行")
    void read_shouldParseUploadedFile() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtils.write(response, "x.xlsx", "s", DemoVO.class,
                List.of(new DemoVO("a", "甲"), new DemoVO("b", "乙"), new DemoVO("c", "丙")));

        MockMultipartFile file = new MockMultipartFile("file", "x.xlsx",
                "application/vnd.ms-excel", response.getContentAsByteArray());

        assertThat(ExcelUtils.read(file, DemoVO.class)).hasSize(3);
    }

    @Test
    @DisplayName("空数据导出应产生只有表头的文件，而不是报错")
    void write_shouldHandleEmptyData() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ExcelUtils.write(response, "empty.xlsx", "s", DemoVO.class, List.of());

        assertThat(ExcelUtils.read(new ByteArrayInputStream(response.getContentAsByteArray()), DemoVO.class))
                .isEmpty();
    }
}

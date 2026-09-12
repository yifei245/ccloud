package com.ccloud.framework.excel.core.util;

import cn.idev.excel.FastExcel;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导入导出工具
 */
public class ExcelUtils {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 导出到响应流
     *
     * @param filename 文件名，含中文时会自动编码
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 中文文件名必须编码，否则部分浏览器下载下来是乱码
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + encoded + ";filename*=utf-8''" + encoded);

        FastExcel.write(response.getOutputStream(), head)
                // 按内容自适应列宽，避免导出的表格需要手动拉宽才能看
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheetName)
                .doWrite(data);
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        return read(file.getInputStream(), head);
    }

    public static <T> List<T> read(InputStream inputStream, Class<T> head) {
        return FastExcel.read(inputStream, head, null)
                .autoCloseStream(false)
                .doReadAllSync();
    }
}

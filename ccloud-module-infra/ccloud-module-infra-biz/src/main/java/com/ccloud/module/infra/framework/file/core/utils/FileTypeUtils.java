package com.ccloud.module.infra.framework.file.core.utils;

import org.apache.tika.Tika;

import java.util.Map;

/**
 * 按内容识别文件类型，只放行白名单。
 *
 * <p>扩展名与请求头里的类型都由上传者决定，不可信；网页、SVG、XML、脚本即使改名也会被识别出来并拒绝，
 * 否则上传的文件经我们的域名输出，就成了存储型 XSS 的载体
 */
public final class FileTypeUtils {

    private static final Tika TIKA = new Tika();

    /**
     * 允许上传的类型 → 保存时使用的扩展名
     */
    private static final Map<String, String> ALLOWED = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"), Map.entry("image/png", "png"), Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp"), Map.entry("image/bmp", "bmp"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-excel", "xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("application/zip", "zip"),
            Map.entry("text/plain", "txt"), Map.entry("text/csv", "csv"),
            Map.entry("audio/mpeg", "mp3"), Map.entry("video/mp4", "mp4"));

    public record FileType(String mimeType, String extension) {
    }

    private FileTypeUtils() {
    }

    /**
     * @param name 原始文件名，只作为同类格式的细分依据（如 zip 与 docx），可为空
     * @return 白名单内的类型；不在白名单返回 null
     */
    public static FileType detect(byte[] content, String name) {
        String mimeType = detectMimeType(content, name);
        String extension = ALLOWED.get(mimeType);
        return extension != null ? new FileType(mimeType, extension) : null;
    }

    /**
     * @return Tika 的原始识别结果，不限于白名单，用于错误提示
     */
    public static String detectMimeType(byte[] content, String name) {
        return TIKA.detect(content, name);
    }

    public static boolean isInline(String mimeType) {
        return mimeType != null && (mimeType.startsWith("image/") || mimeType.equals("application/pdf"));
    }
}

package com.ccloud.framework.common.util.io;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具
 */
public class FileUtils {

    /**
     * 把字节数组写入临时文件，返回文件路径。
     * 用于必须以 File 为入参的第三方 SDK。
     */
    public static String writeToTempFile(byte[] data, String suffix) throws IOException {
        File file = File.createTempFile("ccloud-", StrUtil.isBlank(suffix) ? ".tmp" : suffix);
        file.deleteOnExit();
        FileUtil.writeBytes(data, file);
        return file.getAbsolutePath();
    }

    /**
     * 依据文件内容而非扩展名判断类型。扩展名可以伪造，内容不行。
     */
    public static String getFileType(byte[] data, String fileName) {
        try (InputStream in = new ByteArrayInputStream(data)) {
            return FileTypeUtil.getType(in, fileName);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getFileExtension(String fileName) {
        return FileUtil.extName(fileName);
    }

    public static byte[] readBytes(InputStream in) {
        return IoUtil.readBytes(in);
    }

    /**
     * 生成带路径的唯一文件名，避免同名覆盖
     */
    public static String generatePath(byte[] content, String originalName) {
        String sha256 = cn.hutool.crypto.digest.DigestUtil.sha256Hex(content);
        String ext = getFileExtension(originalName);
        return StrUtil.isBlank(ext) ? sha256 : sha256 + "." + ext;
    }
}

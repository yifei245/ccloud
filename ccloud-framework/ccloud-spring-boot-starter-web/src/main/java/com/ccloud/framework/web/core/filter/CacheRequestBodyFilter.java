package com.ccloud.framework.web.core.filter;

import com.ccloud.framework.common.util.servlet.ServletUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体，使其可被重复读取。
 *
 * <p>Servlet 的输入流只能读一次，而访问日志、开放 API 签名校验、
 * Controller 参数绑定三方都要读 body，必须先缓存。
 */
public class CacheRequestBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new CacheRequestBodyWrapper(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只缓存 JSON 请求体：文件上传的 multipart 流可能非常大，缓存会打爆内存
        return !ServletUtils.isJsonRequest(request);
    }

    /**
     * 把请求体读进内存，之后每次 getInputStream 都基于该副本重新构造流
     */
    public static class CacheRequestBodyWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        public CacheRequestBodyWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = StreamUtils.copyToByteArray(request.getInputStream());
        }

        public byte[] getBody() {
            return body;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener listener) {
                    throw new UnsupportedOperationException("不支持异步读取");
                }

                @Override
                public int available() {
                    return in.available();
                }
            };
        }
    }
}

package com.chat.aspect;

import com.alibaba.fastjson.JSONObject;
import com.chat.util.SM4Util;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.RequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * 请求加解密过滤器
 *
 * @author 猴哥
 */
@Component
public class RequestHandler implements Filter {

    Logger log = org.slf4j.LoggerFactory.getLogger(RequestHandler.class);

    /**
     * 进行请求解密
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // form-data不校验
        if ("application/x-www-form-urlencoded".equals(request.getContentType())) {
            chain.doFilter(request, response);
            return;
        }

        // 拿到加密串
        // 如果没有使用 ContentCachingRequestWrapper，则手动读取请求体
        StringBuilder data = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (IOException e) {
            log.error("Failed to read request body", e);
        }

        if (StringUtils.isEmpty(data)) {
            chain.doFilter(request, response);
            return;
        }
        Map parse = JSONObject.parseObject(data.toString(), Map.class);
        if (null == parse) {
            chain.doFilter(request, response);
            return;
        }

        Object body = parse.get("data");
        if (null == body) {
            chain.doFilter(request, response);
            return;
        }

        // 解析
        request = new BodyRequestWrapper((HttpServletRequest) request, SM4Util.decrypt(body.toString()));
        chain.doFilter(request, response);
    }
}

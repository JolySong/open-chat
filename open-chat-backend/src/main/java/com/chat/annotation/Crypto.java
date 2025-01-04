package com.chat.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Crypto {
    /**
     * 是否解密请求参数
     */
    boolean decryptRequest() default true;

    /**
     * 是否加密响应数据
     */
    boolean encryptResponse() default true;
} 
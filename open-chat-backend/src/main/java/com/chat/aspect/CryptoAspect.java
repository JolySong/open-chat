package com.chat.aspect;

import com.chat.annotation.Crypto;
import com.chat.model.dto.MessageDTO;
import com.chat.model.vo.ApiResponse;
import com.chat.util.SM4Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(-1)
public class CryptoAspect {
    
    private final ObjectMapper objectMapper;

//    @Around("@annotation(crypto)")
//    public Object around(ProceedingJoinPoint point, Crypto crypto) throws Throwable {
//        Object[] args = point.getArgs();
//
//
//        // 执行原方法
//        Object result = point.proceed(args);
//
//        // 响应结果加密
//        if (crypto.encryptResponse() && result != null) {
//            if (result instanceof ApiResponse) {
//                ApiResponse<?> response = (ApiResponse<?>) result;
//                ApiResponse<String> resp = new ApiResponse<>();
//                BeanUtils.copyProperties(response, resp);
//
//                if (response.getData() != null) {
//                    try {
//                        String json = objectMapper.writeValueAsString(response.getData());
//                        log.debug("Original response data: {}", json);
//                        String encrypted = SM4Util.encrypt(json);
//                        log.debug("Encrypted response data: {}", encrypted);
//                        resp.setData(encrypted);
//                        result = resp;
//                    } catch (Exception e) {
//                        log.error("Failed to encrypt/serialize response data", e);
//                        throw e;
//                    }
//                }
//                return result;
//            }
//            // 如果不是ApiResponse，直接加密整个结果
//            try {
//                String json = objectMapper.writeValueAsString(result);
//                return SM4Util.encrypt(json);
//            } catch (Exception e) {
//                log.error("Failed to encrypt/serialize direct response", e);
//                throw e;
//            }
//        }
//
//        return result;
//    }


    @Around("@annotation(crypto)")
    public Object around(ProceedingJoinPoint point, Crypto crypto) throws Throwable {
        Object[] args = point.getArgs();

        // 执行原方法
        Object result = point.proceed(args);

        // 响应结果加密
        if (crypto.encryptResponse() && result != null) {
            if (result instanceof DeferredResult<?>) {
                DeferredResult<?> deferredResult = (DeferredResult<?>) result;
                Object deferredResultValue = deferredResult.getResult();
                DeferredResult<ApiResponse> result1 = new DeferredResult<>();
                BeanUtils.copyProperties(deferredResult, result1);
                if (deferredResultValue instanceof ApiResponse) {
                    ApiResponse<?> response = (ApiResponse<?>) deferredResultValue;
                    ApiResponse<String> resp = new ApiResponse<>();
                    BeanUtils.copyProperties(response, resp);

                    if (response.getData() != null) {
                        try {
                            String json = objectMapper.writeValueAsString(response.getData());
                            log.debug("Original response data: {}", json);
                            String encrypted = SM4Util.encrypt(json);
                            log.debug("Encrypted response data: {}", encrypted);
                            resp.setData(encrypted);
                            result1.setResult(ApiResponse.success(encrypted));
                            deferredResultValue = result1;
                        } catch (Exception e) {
                            log.error("Failed to encrypt/serialize response data", e);
                            throw e;
                        }
                    }
                } else {
                    // 如果不是 ApiResponse，直接加密整个结果
                    try {
                        String json = objectMapper.writeValueAsString(deferredResultValue);
                        result1.setResult(ApiResponse.success(SM4Util.encrypt(json)));
                        deferredResultValue = result1;
                    } catch (Exception e) {
                        log.error("Failed to encrypt/serialize direct response", e);
                        throw e;
                    }
                }

                return deferredResult;
            } else if (result instanceof ApiResponse) {
                ApiResponse<?> response = (ApiResponse<?>) result;
                ApiResponse<String> resp = new ApiResponse<>();
                BeanUtils.copyProperties(response, resp);

                if (response.getData() != null) {
                    try {
                        String json = objectMapper.writeValueAsString(response.getData());
                        log.debug("Original response data: {}", json);
                        String encrypted = SM4Util.encrypt(json);
                        log.debug("Encrypted response data: {}", encrypted);
                        resp.setData(encrypted);
                        return resp;
                    } catch (Exception e) {
                        log.error("Failed to encrypt/serialize response data", e);
                        throw e;
                    }
                }
                return result;
            } else {
                // 如果不是 ApiResponse 或 DeferredResult，直接加密整个结果
                try {
                    String json = objectMapper.writeValueAsString(result);
                    return SM4Util.encrypt(json);
                } catch (Exception e) {
                    log.error("Failed to encrypt/serialize direct response", e);
                    throw e;
                }
            }
        }

        return result;
    }

} 
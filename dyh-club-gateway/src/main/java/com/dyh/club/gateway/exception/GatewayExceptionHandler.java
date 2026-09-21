package com.dyh.club.gateway.exception;

import cn.dev33.satoken.exception.SaTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dyh.club.gateway.entity.Result;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理
 *
 */
@Component
@Slf4j
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    /**
     * 在微服务架构中，Gateway 是所有请求的“大门”。如果请求在网关层就报错了（比如：没带 Token 导致 Sa-Token 鉴权失败，或者网关自身的限流、路由配置出错），由于此时请求还没有到达后端的真实微服务（如 Subject、Auth），后端微服务的异常处理是抓不到它的。
     * 因此，网关必须有自己的一套异常处理机制，**把冷冰冰的底层报错，包装成前端能看懂的、统一格式的 JSON 字符串返回去**。这就是这个函数的核心作用。
     */

    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
        ServerHttpRequest request = serverWebExchange.getRequest();
        ServerHttpResponse response = serverWebExchange.getResponse();
        Integer code = 200;
        String message = "";
        if (throwable instanceof SaTokenException) {//如果是SaTokenException抛出的错误
            code = 401;
            message = "用户无权限";
        } else {
            code = 500;
            message = "系统繁忙";
            log.error("网关请求处理失败，path={}",request.getURI().getPath(),throwable);
        }
        Result result = Result.fail(code, message);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);//告诉浏览器：“我接下来给你发的是 JSON”（setContentType）。
        response.setStatusCode(code==401?HttpStatus.UNAUTHORIZED:HttpStatus.INTERNAL_SERVER_ERROR);
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            byte[] bytes = null;
            try {
                bytes = objectMapper.writeValueAsBytes(result);
            } catch (JsonProcessingException e) {
                log.error("网关异常响应序列化失败",e);
                bytes = "{\"code\":500,\"message\":\"系统繁忙\"}".getBytes(StandardCharsets.UTF_8);
            }
            return dataBufferFactory.wrap(bytes);
        }));
    }

}

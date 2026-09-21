package com.dyh.club.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 登录拦截器，用于用户上下文打通
 *
 */
@Component
@Slf4j
public class LoginFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate().headers(headers -> headers.remove("loginId"));
        String url = request.getURI().getPath();
        if (StpUtil.isLogin()) {
            mutate.header("loginId", StpUtil.getLoginIdAsString());
        }
        log.debug("网关身份上下文处理完成，path={}, authenticated={}", url, StpUtil.isLogin());
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }
}

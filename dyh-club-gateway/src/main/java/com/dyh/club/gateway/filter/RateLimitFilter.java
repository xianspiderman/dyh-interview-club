package com.dyh.club.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {
    private static final RedisScript<Long> WINDOW = RedisScript.of(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('PEXPIRE',KEYS[1],ARGV[1]) end; return n", Long.class);
    private static final RedisScript<Long> TOKEN_BUCKET = RedisScript.of(
            "local now=tonumber(ARGV[1]); local rate=tonumber(ARGV[2]); local cap=tonumber(ARGV[3]); local period=tonumber(ARGV[4]); local v=redis.call('hmget',KEYS[1],'tokens','last'); local tokens=tonumber(v[1]); local last=tonumber(v[2]); if not tokens then tokens=cap end; if not last then last=now end; tokens=math.min(cap,tokens+math.max(0,now-last)*rate/period); local ok=0; if tokens>=1 then tokens=tokens-1; ok=1 end; redis.call('hmset',KEYS[1],'tokens',tokens,'last',now); redis.call('pexpire',KEYS[1],math.ceil(cap/rate*period*2)); return ok",Long.class);
    private final ReactiveStringRedisTemplate redis;
    public RateLimitFilter(ReactiveStringRedisTemplate redis){this.redis=redis;}
    @Override public int getOrder(){return -90;}
    @Override public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){
        Rule rule=rule(exchange.getRequest());if(rule==null)return chain.filter(exchange);
        String path=exchange.getRequest().getURI().getPath();String identity=StpUtil.isLogin()?"u:"+StpUtil.getLoginIdAsString():"ip:"+clientIp(exchange);
        long now=System.currentTimeMillis(),second=now/1000;
        String globalKey="club:limit:global:"+rule.name+":"+second;String userKey="club:limit:user:"+rule.name+":"+identity;
        return allowed(globalKey,1000,rule.globalLimit).flatMap(global->{if(!global)return reject(exchange,rule.name);
            return tokenAllowed(userKey,now,rule.userRate,rule.userCapacity,rule.userPeriodMillis).flatMap(user->user?chain.filter(exchange):reject(exchange,rule.name));
        }).onErrorResume(error->{log.warn("限流组件异常，按可用性策略放行 path={}",path,error);return chain.filter(exchange);});
    }
    private Mono<Boolean> allowed(String key,long ttl,long limit){return redis.execute(WINDOW,Collections.singletonList(key),Collections.singletonList(String.valueOf(ttl))).next().defaultIfEmpty(1L).map(n->n<=limit);}
    private Mono<Boolean> tokenAllowed(String key,long now,long rate,long capacity,long period){return redis.execute(TOKEN_BUCKET,Collections.singletonList(key),Arrays.asList(String.valueOf(now),String.valueOf(rate),String.valueOf(capacity),String.valueOf(period))).next().defaultIfEmpty(1L).map(n->n==1);}
    private Mono<Void> reject(ServerWebExchange exchange,String scene){ServerHttpResponse response=exchange.getResponse();response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);response.getHeaders().setContentType(MediaType.APPLICATION_JSON);response.getHeaders().set("Retry-After","1");byte[] bytes=("{\"code\":429,\"message\":\"操作过于频繁，请稍后重试\",\"scene\":\""+scene+"\"}").getBytes(StandardCharsets.UTF_8);DataBuffer buffer=response.bufferFactory().wrap(bytes);return response.writeWith(Mono.just(buffer));}
    private String clientIp(ServerWebExchange exchange){String forwarded=exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");if(forwarded!=null&&!forwarded.trim().isEmpty())return forwarded.split(",")[0].trim();return exchange.getRequest().getRemoteAddress()==null?"unknown":exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();}
    private Rule rule(ServerHttpRequest request){String path=request.getURI().getPath();if(path.endsWith("/login")||path.endsWith("/doLogin"))return new Rule("login",20,5,5,60000);if(path.contains("/search"))return new Rule("search",200,10,20,1000);if(path.matches(".*/practices/\\d+/submit$"))return new Rule("submit",30,1,2,1000);if(path.matches(".*/questions/\\d+/like$"))return new Rule("like",100,2,4,1000);return null;}
    private static class Rule{final String name;final long globalLimit,userRate,userCapacity,userPeriodMillis;Rule(String n,long g,long r,long c,long p){name=n;globalLimit=g;userRate=r;userCapacity=c;userPeriodMillis=p;}}
}

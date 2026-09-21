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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {
    static final String TOKEN_BUCKET_SCRIPT="local now=tonumber(ARGV[1]); local rate=tonumber(ARGV[2]); local cap=tonumber(ARGV[3]); local period=tonumber(ARGV[4]); local v=redis.call('hmget',KEYS[1],'tokens','last'); local tokens=tonumber(v[1]); local last=tonumber(v[2]); if not tokens then tokens=cap end; if not last then last=now end; tokens=math.min(cap,tokens+math.max(0,now-last)*rate/period); local ok=0; if tokens>=1 then tokens=tokens-1; ok=1 end; redis.call('hmset',KEYS[1],'tokens',tokens,'last',now); redis.call('pexpire',KEYS[1],math.ceil(cap/rate*period*2)); return ok";
    private static final RedisScript<Long> TOKEN_BUCKET = RedisScript.of(TOKEN_BUCKET_SCRIPT,Long.class);
    private final ReactiveStringRedisTemplate redis;
    private final boolean trustForwardedFor;
    public RateLimitFilter(ReactiveStringRedisTemplate redis,@Value("${club.gateway.trust-forwarded-for:false}")boolean trustForwardedFor){this.redis=redis;this.trustForwardedFor=trustForwardedFor;}
    @Override public int getOrder(){return -90;}
    @Override public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){
        Rule rule=rule(exchange.getRequest());if(rule==null)return chain.filter(exchange);
        String path=exchange.getRequest().getURI().getPath();String identity;
        try{identity=StpUtil.isLogin()?"u:"+StpUtil.getLoginIdAsString():"ip:"+clientIp(exchange);}catch(Exception sessionError){if(rule.failOpen){log.warn("搜索限流读取 Session 失败，按只读可用性策略放行 path={}",path,sessionError);return chain.filter(exchange);}log.error("高风险接口读取 Session 失败，执行失败关闭 path={}",path,sessionError);return unavailable(exchange,rule.name);}
        long now=System.currentTimeMillis();
        String globalKey="club:limit:global:"+rule.name;String userKey="club:limit:user:"+rule.name+":"+identity;
        return tokenAllowed(globalKey,now,rule.globalLimit,rule.globalLimit,1000).flatMap(global->{if(!global)return reject(exchange,rule.name);
            return tokenAllowed(userKey,now,rule.userRate,rule.userCapacity,rule.userPeriodMillis).flatMap(user->user?chain.filter(exchange):reject(exchange,rule.name));
        }).onErrorResume(error->{if(rule.failOpen){log.warn("搜索限流 Redis 异常，按只读可用性策略放行 path={}",path,error);return chain.filter(exchange);}log.error("高风险接口限流 Redis 异常，执行失败关闭 path={}",path,error);return unavailable(exchange,rule.name);});
    }
    private Mono<Boolean> tokenAllowed(String key,long now,long rate,long capacity,long period){return redis.execute(TOKEN_BUCKET,Collections.singletonList(key),Arrays.asList(String.valueOf(now),String.valueOf(rate),String.valueOf(capacity),String.valueOf(period))).next().defaultIfEmpty(0L).map(n->n==1);}
    private Mono<Void> reject(ServerWebExchange exchange,String scene){ServerHttpResponse response=exchange.getResponse();response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);response.getHeaders().setContentType(MediaType.APPLICATION_JSON);response.getHeaders().set("Retry-After","1");byte[] bytes=("{\"code\":429,\"message\":\"操作过于频繁，请稍后重试\",\"scene\":\""+scene+"\"}").getBytes(StandardCharsets.UTF_8);DataBuffer buffer=response.bufferFactory().wrap(bytes);return response.writeWith(Mono.just(buffer));}
    private Mono<Void> unavailable(ServerWebExchange exchange,String scene){ServerHttpResponse response=exchange.getResponse();response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);response.getHeaders().setContentType(MediaType.APPLICATION_JSON);byte[] bytes=("{\"code\":503,\"message\":\"安全限流组件暂不可用，请稍后重试\",\"scene\":\""+scene+"\"}").getBytes(StandardCharsets.UTF_8);return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));}
    private String clientIp(ServerWebExchange exchange){String forwarded=trustForwardedFor?exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"):null;if(forwarded!=null&&!forwarded.trim().isEmpty())return forwarded.split(",")[0].trim();return exchange.getRequest().getRemoteAddress()==null?"unknown":exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();}
    static Rule rule(ServerHttpRequest request){String path=request.getURI().getPath();if(path.endsWith("/login")||path.endsWith("/doLogin"))return new Rule("login",20,5,5,60000,false);if(path.contains("/search"))return new Rule("search",200,10,20,1000,true);if(path.matches(".*/practices/\\d+/submit$"))return new Rule("submit",30,1,2,1000,false);if(path.matches(".*/questions/\\d+/like$"))return new Rule("like",100,2,4,1000,false);return null;}
    static class Rule{final String name;final long globalLimit,userRate,userCapacity,userPeriodMillis;final boolean failOpen;Rule(String n,long g,long r,long c,long p,boolean f){name=n;globalLimit=g;userRate=r;userCapacity=c;userPeriodMillis=p;failOpen=f;}}
}

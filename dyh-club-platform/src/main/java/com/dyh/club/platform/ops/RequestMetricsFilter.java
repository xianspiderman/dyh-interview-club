package com.dyh.club.platform.ops;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(20)
public class RequestMetricsFilter extends OncePerRequestFilter {
    private final MeterRegistry registry;
    public RequestMetricsFilter(MeterRegistry registry){this.registry=registry;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        Timer.Sample sample=Timer.start(registry);try{chain.doFilter(request,response);}finally{
            String group=request.getRequestURI().replaceAll("/\\d+","/{id}");
            sample.stop(Timer.builder("club.http.requests").tag("method",request.getMethod()).tag("uri",group)
                    .tag("status",String.valueOf(response.getStatus())).publishPercentiles(0.5,0.95).publishPercentileHistogram().register(registry));
        }
    }
}

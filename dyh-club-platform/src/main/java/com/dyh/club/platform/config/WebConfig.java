package com.dyh.club.platform.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.dyh.club.platform.common.UserContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter.match("/api/**")
                .notMatch("/api/auth/register", "/api/auth/login", "/api/questions/**", "/api/catalog/**",
                        "/api/community/circles", "/api/community/posts", "/api/community/posts/*",
                        "/api/community/comments", "/api/community/replies", "/api/search/**", "/api/ops/**")
                .check(r -> StpUtil.checkLogin()))).addPathPatterns("/**");
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){if(StpUtil.isLogin())UserContext.set(StpUtil.getLoginIdAsLong());return true;}
            @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception ex){UserContext.clear();}
        }).addPathPatterns("/api/**");
    }
}

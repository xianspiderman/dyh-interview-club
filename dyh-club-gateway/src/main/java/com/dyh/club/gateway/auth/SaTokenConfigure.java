package com.dyh.club.gateway.auth;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 权限认证的配置器
 *
 */
@Configuration
public class SaTokenConfigure {

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    // 登录校验 -- 拦截所有路由，并排除/user/doLogin 用于开放登录
//                    SaRouter.match("/auth/**", "/auth/user/doLogin", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/oss/**", r -> StpUtil.checkLogin());

                    // StpUtil.checkRole("admin")是校验是否是 “管理员” 角色
                    SaRouter.match("/subject/subject/add", r -> StpUtil.checkPermission("subject:add"));
                    SaRouter.match("/subject/**", r -> StpUtil.checkLogin());//
                    SaRouter.match("/api/auth/logout", "/api/auth/me", r -> StpUtil.checkLogin());
                    SaRouter.match("/api/practices/**", r -> StpUtil.checkLogin());
                    SaRouter.match("/api/admin/**", r -> StpUtil.checkLogin());
                    SaRouter.match("/api/community/posts", r -> {if("POST".equalsIgnoreCase(cn.dev33.satoken.context.SaHolder.getRequest().getMethod()))StpUtil.checkLogin();});
                    SaRouter.match("/api/community/comments", r -> {if("POST".equalsIgnoreCase(cn.dev33.satoken.context.SaHolder.getRequest().getMethod()))StpUtil.checkLogin();});
                    SaRouter.match("/api/questions/*/like", r -> {if(!"GET".equalsIgnoreCase(cn.dev33.satoken.context.SaHolder.getRequest().getMethod()))StpUtil.checkLogin();});
                    SaRouter.match("/api/search/**", r -> {if(!"GET".equalsIgnoreCase(cn.dev33.satoken.context.SaHolder.getRequest().getMethod()))StpUtil.checkLogin();});
                })
                ;
    }
}

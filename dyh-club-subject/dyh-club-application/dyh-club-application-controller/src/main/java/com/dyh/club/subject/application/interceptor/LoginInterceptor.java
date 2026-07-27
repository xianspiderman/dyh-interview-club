package com.dyh.club.subject.application.interceptor;

import com.dyh.club.subject.common.context.LoginContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String loginId = request.getHeader("loginId"); //先得到loginId
        if (StringUtils.isNotBlank(loginId)) {
            LoginContextHolder.set("loginId", loginId); //根据步骤放到上下文对象LoginContextHolder
        }
        return true;
    }

    @Override //用完移除
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        LoginContextHolder.remove();
    }

}

package com.dyh.club.practice.server.util;

import com.dyh.club.practice.server.config.context.LoginContextHolder;

/**
 * 用户登录util
 *
 */
public class LoginUtil {

    public static String getLoginId() {
        return LoginContextHolder.getLoginId();
    }


}

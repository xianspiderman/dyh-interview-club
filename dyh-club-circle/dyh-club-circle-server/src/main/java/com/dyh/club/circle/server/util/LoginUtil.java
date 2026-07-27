package com.dyh.club.circle.server.util;

import com.dyh.club.circle.server.config.context.LoginContextHolder;

/**
 * 用户登录util
 *
 */
public class LoginUtil {

    public static String getLoginId() {
        return LoginContextHolder.getLoginId();
    }


}

package com.dyh.club.interview.server.util;

import com.dyh.club.interview.server.config.context.LoginContextHolder;

/**
 * 用户登录util
 *
 */
public class LoginUtil {

    public static String getLoginId() {
        return LoginContextHolder.getLoginId();
    }


}

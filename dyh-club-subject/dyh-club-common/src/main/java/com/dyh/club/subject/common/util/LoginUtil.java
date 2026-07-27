package com.dyh.club.subject.common.util;


import com.dyh.club.subject.common.context.LoginContextHolder;

/**
 * 用户登录util
 *
 */
public class LoginUtil {

    public static String getLoginId() {
        return LoginContextHolder.getLoginId();
    }


}

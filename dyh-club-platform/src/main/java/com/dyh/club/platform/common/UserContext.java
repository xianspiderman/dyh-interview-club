package com.dyh.club.platform.common;

public final class UserContext {
    private static final ThreadLocal<Long> USER_ID=new ThreadLocal<>();
    private UserContext(){}
    public static Long get(){return USER_ID.get();}
    public static void set(Long userId){if(userId==null)USER_ID.remove();else USER_ID.set(userId);}
    public static void clear(){USER_ID.remove();}
}

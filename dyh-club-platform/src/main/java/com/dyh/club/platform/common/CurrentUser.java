package com.dyh.club.platform.common;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final JdbcTemplate jdbc;

    public CurrentUser(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long id() { Long context=UserContext.get();return context==null?StpUtil.getLoginIdAsLong():context; }

    public Long optionalId() {
        Long context=UserContext.get();return context!=null?context:(StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null);
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw BizException.forbidden("仅管理员可执行此操作");
        }
    }

    public boolean isAdmin() {
        String role = jdbc.queryForObject("SELECT role_code FROM club_user WHERE id=?", String.class, id());
        return "ADMIN".equals(role);
    }
}

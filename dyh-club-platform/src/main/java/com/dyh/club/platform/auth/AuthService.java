package com.dyh.club.platform.auth;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.dyh.club.platform.common.BizException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Order(1)
public class AuthService implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final boolean demoEnabled;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public AuthService(JdbcTemplate jdbc,@Value("${club.demo.enabled:false}")boolean demoEnabled) { this.jdbc = jdbc;this.demoEnabled=demoEnabled; }

    @Override
    public void run(String... args) {
        if(!demoEnabled)return;
        createDemoUser("club-admin", "Club@123", "演示管理员", "ADMIN");
        createDemoUser("club-user", "Club@123", "演示用户", "USER");
    }

    private void createDemoUser(String username, String password, String nickname, String role) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM club_user WHERE username=?", Integer.class, username);
        if (count != null && count == 0) {
            jdbc.update("INSERT INTO club_user(username,password_hash,nickname,role_code,status) VALUES(?,?,?,?, 'ACTIVE')",
                    username, encoder.encode(password), nickname, role);
        }
    }

    @Transactional
    public Map<String, Object> register(String username, String password, String nickname) {
        if (username == null || !username.matches("[A-Za-z0-9_-]{4,32}")) {
            throw BizException.badRequest("用户名需为 4 到 32 位字母、数字、下划线或短横线");
        }
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw BizException.badRequest("密码长度需为 8 到 64 位");
        }
        if (nickname == null || nickname.trim().isEmpty() || nickname.length() > 32) {
            throw BizException.badRequest("昵称不能为空且不能超过 32 个字符");
        }
        try {
            jdbc.update("INSERT INTO club_user(username,password_hash,nickname,role_code,status) VALUES(?,?,?,'USER','ACTIVE')",
                    username, encoder.encode(password), nickname.trim());
        } catch (DuplicateKeyException duplicate) {
            throw BizException.conflict("用户名已存在");
        }
        Long id = jdbc.queryForObject("SELECT id FROM club_user WHERE username=?", Long.class, username);
        return user(id);
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> row;
        try {
            row = jdbc.queryForMap("SELECT id,username,password_hash,nickname,avatar,role_code,status FROM club_user WHERE username=?", username);
        } catch (org.springframework.dao.EmptyResultDataAccessException missing) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(row.get("status")) || !encoder.matches(password, String.valueOf(row.get("password_hash")))) {
            throw new BizException(401, "用户名或密码错误");
        }
        long id = ((Number) row.get("id")).longValue();
        StpUtil.login(id);
        SaTokenInfo token = StpUtil.getTokenInfo();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tokenName", token.getTokenName());
        result.put("tokenValue", token.getTokenValue());
        result.put("user", user(id));
        return result;
    }

    public void logout() { StpUtil.logout(); }

    public Map<String, Object> user(long id) {
        return jdbc.queryForObject("SELECT id,username,nickname,avatar,role_code,status,created_at FROM club_user WHERE id=?",
                (rs, rowNum) -> {
                    Map<String, Object> user = new LinkedHashMap<>();
                    user.put("id", rs.getLong("id"));
                    user.put("username", rs.getString("username"));
                    user.put("nickname", rs.getString("nickname"));
                    user.put("avatar", rs.getString("avatar"));
                    user.put("role", rs.getString("role_code"));
                    user.put("status", rs.getString("status"));
                    user.put("createdAt", rs.getTimestamp("created_at"));
                    return user;
                }, id);
    }
}

package com.aiteacher.service;

import com.aiteacher.common.R;
import com.aiteacher.dto.LoginRequest;
import com.aiteacher.dto.LoginResponse;
import com.aiteacher.entity.User;
import com.aiteacher.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // RowMapper for User entity
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setTenantId(rs.getLong("tenant_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setDeleted(rs.getBoolean("deleted"));
        return user;
    };

    public R<LoginResponse> login(LoginRequest request) {
        // Use JdbcTemplate to bypass MyBatis wrapper issue
        String sql = "SELECT id, tenant_id, username, password, email, role, enabled, deleted FROM users WHERE username = ? AND deleted = false";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, request.getUsername());

        if (users.isEmpty()) {
            return R.fail("用户名或密码错误");
        }

        User user = users.get(0);

        if (!user.getEnabled()) {
            return R.fail("账号已被禁用");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return R.fail("用户名或密码错误");
        }

        // Generate Token
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());

        LoginResponse response = new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getId()
        );

        return R.ok("登录成功", response);
    }
}
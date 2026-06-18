package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.dto.LoginRequest;
import com.aiteacher.dto.LoginResponse;
import com.aiteacher.dto.RegisterRequest;
import com.aiteacher.entity.User;
import com.aiteacher.mapper.UserMapper;
import com.aiteacher.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DataSource dataSource;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user and return JWT token")
    public R<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Use refresh token to get a new access token")
    public R<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return R.fail("refreshToken is required");
        }
        try {
            String newAccessToken = authService.refreshToken(refreshToken);
            return R.ok("token refreshed", Map.of("token", newAccessToken));
        } catch (Exception e) {
            return R.fail("无效或已过期的 refresh token");
        }
    }

    @GetMapping("/debug/user/{username}")
    @Operation(summary = "Debug: find user - test MyBatis directly")
    public R<Object> debugFindUser(@PathVariable String username) {
        Map<String, Object> result = new HashMap<>();

        // Count all users via MyBatis
        Long count = userMapper.selectCount(null);
        result.put("totalUsers", count);

        // Test JDBC
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT id, username, tenant_id FROM users LIMIT 3"
            );
            List<Map<String, Object>> jdbcUsers = new ArrayList<>();
            while (rs.next()) {
                jdbcUsers.add(Map.of(
                    "id", rs.getLong("id"),
                    "username", rs.getString("username"),
                    "tenantId", rs.getLong("tenant_id")
                ));
            }
            result.put("jdbcUsers", jdbcUsers);
        } catch (SQLException e) {
            result.put("jdbcError", e.getMessage());
        }

        // Test MyBatis with count query
        List<User> allUsers = userMapper.selectList(null);
        result.put("mybatisUsersCount", allUsers.size());
        result.put("mybatisUsers", allUsers.stream().limit(3).map(u ->
            Map.of("id", u.getId(), "username", u.getUsername())
        ).toList());

        // Test specific query
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        // Test specific query with different wrapper types
        LambdaQueryWrapper<User> lambdaWrapper = new LambdaQueryWrapper<>();
        lambdaWrapper.eq(User::getUsername, username);
        List<User> lambdaUsers = userMapper.selectList(lambdaWrapper);
        result.put("lambdaWrapper", lambdaUsers.size() + " users");

        // Test with plain QueryWrapper
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> queryWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("username", username);
        List<User> queryUsers = userMapper.selectList(queryWrapper);
        result.put("queryWrapper", queryUsers.size() + " users");

        result.put("specificQuery", user != null ?
            Map.of("id", user.getId(), "username", user.getUsername()) : "not found");

        return R.ok("Debug", result);
    }

    @GetMapping("/debug/db")
    @Operation(summary = "Debug: direct JDBC connection test")
    public R<Object> debugDb() {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            String user = conn.getMetaData().getUserName();
            ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) as cnt FROM users");
            rs.next();
            int count = rs.getInt("cnt");
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("user", user);
            result.put("usersCount", count);
            return R.ok("DB connection OK", result);
        } catch (SQLException e) {
            return R.fail("DB error: " + e.getMessage());
        }
    }
}
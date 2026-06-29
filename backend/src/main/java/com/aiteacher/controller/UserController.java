package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.dto.ChangePasswordRequest;
import com.aiteacher.dto.UpdateProfileRequest;
import com.aiteacher.entity.User;
import com.aiteacher.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "User management APIs")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // RowMapper for User entity (to avoid MyBatis wrapper issues)
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

    /**
     * Get current user's profile
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the currently authenticated user")
    public R<User> getProfile() {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            return R.fail(401, "未授权");
        }

        String sql = "SELECT id, tenant_id, username, password, email, role, enabled, deleted FROM users WHERE id = ? AND deleted = false";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userId);

        if (users.isEmpty()) {
            return R.fail(404, "用户不存在");
        }

        User user = users.get(0);
        // Don't return the password
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * Update current user's profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Updates the profile of the currently authenticated user")
    public R<User> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            return R.fail(401, "未授权");
        }

        // Validate required fields
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return R.fail(400, "用户名不能为空");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return R.fail(400, "邮箱不能为空");
        }

        // Check if username is taken by another user
        String checkUsernameSql = "SELECT COUNT(*) FROM users WHERE username = ? AND id != ? AND deleted = false";
        Integer usernameCount = jdbcTemplate.queryForObject(checkUsernameSql, Integer.class, request.getUsername(), userId);
        if (usernameCount != null && usernameCount > 0) {
            return R.fail(400, "用户名已被使用");
        }

        // Check if email is taken by another user
        String checkEmailSql = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ? AND deleted = false";
        Integer emailCount = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, request.getEmail(), userId);
        if (emailCount != null && emailCount > 0) {
            return R.fail(400, "邮箱已被使用");
        }

        // Build update query dynamically based on provided fields
        StringBuilder updateSql = new StringBuilder("UPDATE users SET updated_at = NOW()");
        List<Object> params = new java.util.ArrayList<>();

        if (request.getUsername() != null) {
            updateSql.append(", username = ?");
            params.add(request.getUsername());
        }
        if (request.getEmail() != null) {
            updateSql.append(", email = ?");
            params.add(request.getEmail());
        }
        if (request.getPhone() != null) {
            updateSql.append(", phone = ?");
            params.add(request.getPhone());
        }
        if (request.getAvatar() != null) {
            updateSql.append(", avatar = ?");
            params.add(request.getAvatar());
        }

        updateSql.append(" WHERE id = ? AND deleted = false");
        params.add(userId);

        jdbcTemplate.update(updateSql.toString(), params.toArray());

        // Fetch and return updated user
        String selectSql = "SELECT id, tenant_id, username, password, email, role, enabled, deleted FROM users WHERE id = ? AND deleted = false";
        List<User> users = jdbcTemplate.query(selectSql, userRowMapper, userId);

        if (users.isEmpty()) {
            return R.fail(404, "用户不存在");
        }

        User user = users.get(0);
        user.setPassword(null);
        return R.ok("更新成功", user);
    }

    /**
     * Change current user's password
     */
    @PutMapping("/password")
    @Operation(summary = "Change password", description = "Changes the password of the currently authenticated user")
    public R<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            return R.fail(401, "未授权");
        }

        // Validate required fields
        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            return R.fail(400, "旧密码不能为空");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return R.fail(400, "新密码不能为空");
        }
        if (request.getNewPassword().length() < 6) {
            return R.fail(400, "新密码长度不能少于6位");
        }

        // Get current user with password
        String sql = "SELECT id, tenant_id, username, password, email, role, enabled, deleted FROM users WHERE id = ? AND deleted = false";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userId);

        if (users.isEmpty()) {
            return R.fail(404, "用户不存在");
        }

        User user = users.get(0);

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return R.fail(400, "旧密码错误");
        }

        // Encode and update new password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        String updateSql = "UPDATE users SET password = ?, updated_at = NOW() WHERE id = ? AND deleted = false";
        jdbcTemplate.update(updateSql, encodedPassword, userId);

        return R.ok("密码修改成功", null);
    }
}

package com.aiteacher.service;

import com.aiteacher.common.R;
import com.aiteacher.dto.LoginRequest;
import com.aiteacher.dto.LoginResponse;
import com.aiteacher.dto.RegisterRequest;
import com.aiteacher.entity.Tenant;
import com.aiteacher.entity.User;
import com.aiteacher.mapper.TenantMapper;
import com.aiteacher.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private TenantService tenantService;

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

        // Generate tokens
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());

        LoginResponse response = new LoginResponse(
                token,
                refreshToken,
                user.getUsername(),
                user.getRole(),
                user.getId()
        );

        return R.ok("登录成功", response);
    }

    @Transactional
    public R<LoginResponse> register(RegisterRequest request) {
        // Check email uniqueness
        String checkEmailSql = "SELECT COUNT(*) FROM users WHERE email = ? AND deleted = false";
        Integer emailCount = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, request.getEmail());
        if (emailCount != null && emailCount > 0) {
            return R.fail("邮箱已被注册");
        }

        // Check username uniqueness
        String checkUsernameSql = "SELECT COUNT(*) FROM users WHERE username = ? AND deleted = false";
        Integer usernameCount = jdbcTemplate.queryForObject(checkUsernameSql, Integer.class, request.getUsername());
        if (usernameCount != null && usernameCount > 0) {
            return R.fail("用户名已被使用");
        }

        // Determine tenant
        Long tenantId;
        if (request.getTenantName() != null && !request.getTenantName().isBlank()) {
            // Create new tenant
            Tenant tenant = new Tenant();
            tenant.setName(request.getTenantName());
            tenant.setCode(request.getTenantName().toLowerCase().replaceAll("\\s+", "_"));
            tenant.setPlan("free");
            tenant.setQuota("{}");
            tenant = tenantService.create(tenant);
            tenantId = tenant.getId();
        } else {
            // Use default tenant (id=1) or create one if not exists
            LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tenant::getDeleted, false).orderByAsc(Tenant::getId).last("LIMIT 1");
            Tenant defaultTenant = tenantMapper.selectOne(wrapper);
            if (defaultTenant == null) {
                Tenant newTenant = new Tenant();
                newTenant.setName("Default Tenant");
                newTenant.setCode("default");
                newTenant.setPlan("free");
                newTenant.setQuota("{}");
                defaultTenant = tenantService.create(newTenant);
            }
            tenantId = defaultTenant.getId();
        }

        // Encode password and create user
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        String insertSql = "INSERT INTO users (tenant_id, username, password, email, role, enabled, deleted, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, false, NOW(), NOW())";
        jdbcTemplate.update(insertSql, tenantId, request.getUsername(), encodedPassword, 
                           request.getEmail(), "teacher", true);

        // Fetch the created user
        String selectSql = "SELECT id, tenant_id, username, password, email, role, enabled, deleted FROM users WHERE username = ? AND deleted = false";
        List<User> users = jdbcTemplate.query(selectSql, userRowMapper, request.getUsername());
        
        if (users.isEmpty()) {
            return R.fail("用户创建失败");
        }

        User user = users.get(0);

        // Generate tokens (auto login after registration)
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());

        LoginResponse response = new LoginResponse(
                token,
                refreshToken,
                user.getUsername(),
                user.getRole(),
                user.getId()
        );

        return R.ok("注册成功", response);
    }

    public String refreshToken(String refreshToken) {
        return jwtService.refreshAccessToken(refreshToken);
    }
}
package com.aiteacher.service;

import com.aiteacher.common.R;
import com.aiteacher.dto.LoginRequest;
import com.aiteacher.dto.LoginResponse;
import com.aiteacher.entity.User;
import com.aiteacher.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public R<LoginResponse> login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            return R.fail("用户名或密码错误");
        }
        
        if (!user.getEnabled()) {
            return R.fail("账号已被禁用");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return R.fail("用户名或密码错误");
        }
        
        // 生成Token
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
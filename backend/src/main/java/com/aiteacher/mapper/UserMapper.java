package com.aiteacher.mapper;

import com.aiteacher.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, tenant_id, username, password, email, role, enabled, deleted FROM users WHERE username = #{username} AND deleted = false LIMIT 1")
    User selectByUsername(String username);
}
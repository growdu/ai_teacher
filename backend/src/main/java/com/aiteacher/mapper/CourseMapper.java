package com.aiteacher.mapper;

import com.aiteacher.entity.Course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Select("SELECT id, tenant_id, workspace_id, knowledge_point_id, title, outline, script, status, creator_id, created_at, updated_at, deleted FROM course WHERE deleted = false ORDER BY created_at DESC")
    List<Course> selectAllForList();

    @Select("SELECT id, tenant_id, workspace_id, knowledge_point_id, title, outline, script, status, creator_id, created_at, updated_at, deleted FROM course WHERE id = #{id,jdbcType=BIGINT} AND deleted = false")
    Course selectByIdCustom(@Param("id") Long id);
}
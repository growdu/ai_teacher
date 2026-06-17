package com.aiteacher.mapper;

import com.aiteacher.entity.KnowledgePoint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {

    @Select("SELECT id, tenant_id, workspace_id, subject, grade, content, tags, created_at, updated_at, deleted FROM knowledge_point WHERE id = #{id} AND deleted = false")
    KnowledgePoint selectByIdCustom(Long id);

    @Select("SELECT id, tenant_id, workspace_id, subject, grade, content, tags, created_at, updated_at, deleted FROM knowledge_point WHERE deleted = false ORDER BY created_at DESC")
    List<KnowledgePoint> selectAllForList();
}
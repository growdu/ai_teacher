package com.aiteacher.service;

import com.aiteacher.entity.Plan;
import com.aiteacher.mapper.PlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PlanService {

    @Autowired
    private PlanMapper planMapper;

    public List<Plan> getAllActivePlans() {
        return planMapper.selectList(
            new LambdaQueryWrapper<Plan>()
                .eq(Plan::getStatus, "active")
                .orderByAsc(Plan::getPrice)
        );
    }

    public Plan getPlanById(Long id) {
        return planMapper.selectById(id);
    }
}

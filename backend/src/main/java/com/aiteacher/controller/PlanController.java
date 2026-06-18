package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.Plan;
import com.aiteacher.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private PlanService planService;

    /**
     * 获取所有可用套餐
     */
    @GetMapping
    public R<List<Plan>> getAllPlans() {
        List<Plan> plans = planService.getAllActivePlans();
        return R.ok(plans);
    }

    /**
     * 获取单个套餐详情
     */
    @GetMapping("/{id}")
    public R<Plan> getPlanById(@PathVariable Long id) {
        Plan plan = planService.getPlanById(id);
        if (plan == null) {
            return R.fail("套餐不存在");
        }
        return R.ok(plan);
    }
}

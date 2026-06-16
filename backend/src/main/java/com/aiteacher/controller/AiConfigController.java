package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.AiConfig;
import com.aiteacher.service.AIConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-config")
public class AiConfigController {

    @Autowired
    private AIConfigService aiConfigService;

    @GetMapping("/list")
    public R<List<AiConfig>> list() {
        List<AiConfig> configs = aiConfigService.list(
                new LambdaQueryWrapper<AiConfig>()
                        .eq(AiConfig::getDeleted, 0)
                        .orderByDesc(AiConfig::getPriority)
        );
        return R.ok(configs);
    }

    @GetMapping("/page")
    public R<Page<AiConfig>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<AiConfig> page = new Page<>(pageNum, pageSize);
        Page<AiConfig> result = aiConfigService.page(page,
                new LambdaQueryWrapper<AiConfig>()
                        .eq(AiConfig::getDeleted, 0)
                        .orderByDesc(AiConfig::getPriority)
        );
        return R.ok(result);
    }

    @PostMapping
    public R<Boolean> create(@RequestBody AiConfig config) {
        return R.ok(aiConfigService.save(config));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody AiConfig config) {
        config.setId(id);
        return R.ok(aiConfigService.updateById(config));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        AiConfig config = new AiConfig();
        config.setId(id);
        config.setDeleted(true);
        return R.ok(aiConfigService.updateById(config));
    }

    @PostMapping("/reload")
    public R<String> reload() {
        aiConfigService.reloadProviders();
        return R.ok("AI providers reloaded successfully");
    }

    @GetMapping("/status")
    public R<Object> status() {
        return R.ok(new Object(){
            public String llmProvider = aiConfigService.getBestLLMProviderName();
            public String ttsProvider = aiConfigService.getBestTTSProviderName();
            public boolean llmAvailable = aiConfigService.isLLMAvailable();
            public boolean ttsAvailable = aiConfigService.isTTSAvailable();
        });
    }
}
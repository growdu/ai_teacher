package com.aiteacher.service.impl;

import com.aiteacher.service.HealthService;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public String check() {
        return "OK";
    }
}
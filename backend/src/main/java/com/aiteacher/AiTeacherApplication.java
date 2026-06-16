package com.aiteacher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aiteacher.mapper")
public class AiTeacherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTeacherApplication.class, args);
    }
}
package com.yumian;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yumian.mapper")
public class YumianApplication {

    public static void main(String[] args) {
        SpringApplication.run(YumianApplication.class, args);
    }
}

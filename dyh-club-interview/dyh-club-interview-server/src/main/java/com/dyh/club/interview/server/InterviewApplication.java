package com.dyh.club.interview.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 圈子微服务启动类
 *
 */
@SpringBootApplication
@ComponentScan("com.dyh.club")
@MapperScan("com.dyh.club.**.dao")
@EnableFeignClients(basePackages = "com.dyh.club")
public class InterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApplication.class);
    }

}

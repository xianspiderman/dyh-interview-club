package com.dyh.club.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ClubPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClubPlatformApplication.class, args);
    }
}

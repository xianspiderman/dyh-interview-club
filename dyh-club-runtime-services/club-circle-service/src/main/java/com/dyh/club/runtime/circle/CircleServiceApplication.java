package com.dyh.club.runtime.circle;

import com.dyh.club.platform.admin.AdminService;
import com.dyh.club.platform.config.WebConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Import({WebConfig.class, AdminService.class})
@SpringBootApplication(scanBasePackages = {
        "com.dyh.club.runtime.circle",
        "com.dyh.club.platform.community",
        "com.dyh.club.platform.common"
})
public class CircleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CircleServiceApplication.class, args);
    }
}

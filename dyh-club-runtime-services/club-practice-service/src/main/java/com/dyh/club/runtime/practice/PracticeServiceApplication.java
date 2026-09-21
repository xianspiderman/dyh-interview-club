package com.dyh.club.runtime.practice;
import com.dyh.club.platform.config.WebConfig;import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.cloud.openfeign.EnableFeignClients;import org.springframework.context.annotation.Import;
@EnableFeignClients @Import(WebConfig.class)
@SpringBootApplication(scanBasePackages={"com.dyh.club.runtime.practice","com.dyh.club.platform.practice","com.dyh.club.platform.common"})
public class PracticeServiceApplication{public static void main(String[]a){SpringApplication.run(PracticeServiceApplication.class,a);}}

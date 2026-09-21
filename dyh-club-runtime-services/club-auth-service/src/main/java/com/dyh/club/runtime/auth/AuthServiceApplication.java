package com.dyh.club.runtime.auth;
import com.dyh.club.platform.config.WebConfig;
import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.context.annotation.Import;
@Import(WebConfig.class)
@SpringBootApplication(scanBasePackages={"com.dyh.club.platform.auth","com.dyh.club.platform.common"})
public class AuthServiceApplication{public static void main(String[]a){SpringApplication.run(AuthServiceApplication.class,a);}}

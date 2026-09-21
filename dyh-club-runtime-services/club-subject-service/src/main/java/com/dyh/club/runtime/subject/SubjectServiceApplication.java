package com.dyh.club.runtime.subject;
import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication(scanBasePackages={"com.dyh.club.runtime.subject","com.dyh.club.platform.question","com.dyh.club.platform.like","com.dyh.club.platform.search","com.dyh.club.platform.admin","com.dyh.club.platform.ops","com.dyh.club.platform.common","com.dyh.club.platform.config"})
public class SubjectServiceApplication{public static void main(String[]a){SpringApplication.run(SubjectServiceApplication.class,a);}}

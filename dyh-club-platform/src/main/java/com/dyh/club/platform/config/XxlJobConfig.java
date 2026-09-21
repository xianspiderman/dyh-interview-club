package com.dyh.club.platform.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "club.xxl-job", name = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "club.xxl-job")
public class XxlJobConfig {
    private String adminAddresses="http://localhost:8080/xxl-job-admin";
    private String accessToken="";
    private String appName="dyh-club-platform";
    private String address="";
    private String ip="";
    private int port=9999;
    private String logPath="./logs/xxl-job";
    private int logRetentionDays=30;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(){XxlJobSpringExecutor executor=new XxlJobSpringExecutor();executor.setAdminAddresses(adminAddresses);executor.setAccessToken(accessToken);executor.setAppname(appName);executor.setAddress(address);executor.setIp(ip);executor.setPort(port);executor.setLogPath(logPath);executor.setLogRetentionDays(logRetentionDays);return executor;}
    public void setAdminAddresses(String v){adminAddresses=v;}public void setAccessToken(String v){accessToken=v;}public void setAppName(String v){appName=v;}public void setAddress(String v){address=v;}public void setIp(String v){ip=v;}public void setPort(int v){port=v;}public void setLogPath(String v){logPath=v;}public void setLogRetentionDays(int v){logRetentionDays=v;}
}

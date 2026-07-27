package com.dyh.club.oss.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * minio配置管理
 *
 */
/*
1. 第三方类的“入库登记表”
    你可能想问：“我自己写的 UserServiceImpl 类，在头上打个 @Service 注解，Spring 就能自动把它收进仓库，为什么这里不直接给 MinioClient 打注解呢？”
    答案是：MinioClient 是第三方 SDK 里的类，那是别人（MinIO官方）写的源码，你只有 .class 文件，根本无法钻进别人的源码里去加 @Service 或 @Component。
    既然不能在源码上加注解，我们就必须在一个我们自己写的配置类（带有 @Configuration 的 MinioConfig）中，写一个方法，在这个方法头上打上 @Bean。
    @Bean 的潜台词就是告诉 Spring 引擎：
    “嘿，Spring！请在系统启动的时候，主动执行一下 getMinioClient() 这个方法。无论这个方法 return 了什么玩意儿（在这里就是一个构造好的 MinioClient 对象），请把它作为一个 Bean，放进你的核心仓库里妥善保管！”
 */
@Configuration
public class  MinioConfig {

    /**
     * minioUrl
     */
    @Value("${minio.url}")
    private String url;

    /**
     * minio账户
     */
    @Value("${minio.accessKey}")
    private String accessKey;

    /**
     * minio密码
     */
    @Value("${minio.secretKey}")
    private String secretKey;

    /**
     * 构造minioClient
     */
    @Bean
    public MinioClient getMinioClient() {
        return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
    }

}

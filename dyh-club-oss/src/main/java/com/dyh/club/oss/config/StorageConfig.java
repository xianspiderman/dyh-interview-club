package com.dyh.club.oss.config;

import com.dyh.club.oss.adapter.StorageAdapter;
import com.dyh.club.oss.adapter.AliStorageAdapter;
import com.dyh.club.oss.adapter.MinioStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储config
 *
 */

/*
    如果说 application.yml 是你的“图纸”，那么 StorageConfig.java 就是按照图纸把零件组装起来的“工厂流水线”。
    它的核心作用是：利用工厂模式和策略模式，根据你在配置文件里的“开关”，动态地决定系统到底实例化哪一个存储适配器（MinIO 还是 阿里云）。
        1.动态注入策略：它通常会配合 @Value("${storage.type}") 或者 @ConditionalOnProperty 注解。当它发现配置文件写的是 minio，它就会把 MinioStorageAdapter 注册到 Spring 容器里；如果写的是 aliyun，它就注册 AliStorageAdapter。
        2.屏蔽底层差异：通过在这个配置类里统一管理，整个系统的其他部分（比如 FileService）在调用时，根本不需要写 if...else... 来判断。它们只需要注入一个通用的 StorageAdapter 接口即可。
    这种设计让 OSS 模块具备了极强的“插拔性”。
    总结：StorageConfig 是“看人下菜碟”的地方，它决定了系统里哪个存储工具能“转正”生效。
 */
@Configuration
@RefreshScope//nacos自动刷新，如果nacos配置变了，这个会自动刷新
public class StorageConfig {

    @Value("${storage.service.type}")//作者后面用的nacos，就没在application.yml写
    private String storageType;

    @Bean
    @RefreshScope//nacos自动刷新，以后不用minIO,换了配置之后会自动刷新
    public StorageAdapter storageService() {
        if ("minio".equals(storageType)) {
            return new MinioStorageAdapter();
        } else if ("aliyun".equals(storageType)) {
            return new AliStorageAdapter();
        } else {
            throw new IllegalArgumentException("未找到对应的文件存储处理器");
        }
    }

}

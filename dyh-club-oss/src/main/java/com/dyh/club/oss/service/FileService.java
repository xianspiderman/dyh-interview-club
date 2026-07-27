package com.dyh.club.oss.service;

import com.dyh.club.oss.adapter.StorageAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件存储service
 *
 */
/*
    在 dyh-club-subject 模块中，我们看到了大量的接口（Interface）和对应的 Impl。但在 oss 模块中，作者直接写了一个 FileService.java 类，这主要有三个层面的考量：
    1. 业务复杂度的差异（领域服务 vs 基础设施服务）
        Subject 模块：属于核心业务逻辑（Domain），业务逻辑非常复杂且多变。使用接口可以方便地进行分层隔离，也方便后期做 AOP 拦截（比如权限校验、日志记录等）。
        OSS 模块：属于基础设施服务（Infrastructure Service）。它的逻辑极其纯粹：接收请求 -> 调用适配器上传 -> 返回 URL。它本身不需要处理复杂的业务判断，仅仅是一个“搬运工”。在这种“逻辑单一”的情况下，强行写接口（Interface）往往会变成过度设计（Over-Engineering），增加不必要的类搜索成本。
    2. “真接口”已经下沉到了适配器层
        仔细观察你会发现，在这个 OSS 模块中，真正的“契约”和“多态”体现在 StorageAdapter 接口上。
        FileService 实际上只是一个对外的 包装层（Wrapper）。
        核心的差异化实现已经由 MinioStorageAdapter 和 AliStorageAdapter 完成了。
        既然底层已经有了一层接口抽象，那么在 Service 层再加一层接口，就显得有点“套娃”了。
    3. 开发效率与工程直觉
        在微服务架构中，对于这种功能极其确定的辅助型微服务，采用“扁平化”的开发风格（直接写类）能提高代码的阅读速度。如果一个功能一眼就能看穿，就没必要为了“规范”而制造阅读障碍。
        为了让你更直观地理解 StorageConfig 是如何像“开关”一样工作的，我为你准备了一个适配器模式逻辑模拟器。你可以亲自动手切换配置，观察系统是如何动态路由请求的。
    总结笔记：
        不写接口 是因为 OSS 逻辑简单，且真正的抽象已经做在了 StorageAdapter 那一层，没必要为了形式而牺牲简洁。
        这种“该复杂时复杂，该简单时简单”的代码风格，才是真正有经验的架构师所追求的平衡点。你可以接着往下看 FileController 是如何调用这个 FileService 的。
 */
@Service
public class FileService {

    private final StorageAdapter storageAdapter;

    public FileService(StorageAdapter storageAdapter) {
        this.storageAdapter = storageAdapter;
    }

    /**
     * 列出所有桶
     */
    public List<String> getAllBucket() {
        return storageAdapter.getAllBucket();
    }

    /**
     * 获取文件路径
     */
    public String getUrl(String bucketName,String objectName) {
        return storageAdapter.getUrl(bucketName,objectName);
    }

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile uploadFile, String bucket, String objectName){
        storageAdapter.uploadFile(uploadFile,bucket,objectName);
        objectName = objectName + "/" + uploadFile.getOriginalFilename();
        return storageAdapter.getUrl(bucket, objectName);
    }
}


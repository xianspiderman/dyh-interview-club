package com.dyh.club.subject.infra.basic.es;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "es.cluster") //在配置文件里面加载src/main/resources/application.yml
@Data
public class EsConfigProperties {

    private List<EsClusterConfig> esConfigs = new ArrayList<>(); // 新建一个集群类列表

//    public List<EsClusterConfig> getEsConfigs() {
//        return esConfigs;
//    }

//    public void setEsConfigs(List<EsClusterConfig> esConfigs) {
//        this.esConfigs = esConfigs;
//    }
}

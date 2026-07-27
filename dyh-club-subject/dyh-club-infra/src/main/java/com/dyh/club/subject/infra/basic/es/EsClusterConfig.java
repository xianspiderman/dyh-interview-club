package com.dyh.club.subject.infra.basic.es;

import lombok.Data;

import java.io.Serializable;

/**
 * Elasticsearch 集群配置。
 */
@Data
public class EsClusterConfig implements Serializable {

    /**
     * 集群名称
     */
    private String name;

    /**
     * 集群节点
     */
    private String nodes;

}

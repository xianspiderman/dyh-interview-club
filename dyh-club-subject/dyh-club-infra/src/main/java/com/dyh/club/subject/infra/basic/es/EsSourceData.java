package com.dyh.club.subject.infra.basic.es;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class EsSourceData implements Serializable {

    private String docId; // 文档id，唯一的，自动生成

    private Map<String, Object> data; //所有跟EsSourceData交互的封装成一个map

}

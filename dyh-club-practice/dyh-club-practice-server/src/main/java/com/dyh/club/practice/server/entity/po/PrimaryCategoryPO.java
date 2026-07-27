package com.dyh.club.practice.server.entity.po;

import lombok.Data;

@Data
public class PrimaryCategoryPO {
    //分类id
    private Long id;
    //分类名称
    private String categoryName;
    //分类类型，1大类2小类
    private Integer categoryType;

    private Long parentId;

}

package com.dyh.club.practice.server.entity.po;

import lombok.Data;

@Data
public class CategoryPO {

    private Long id;

    private String categoryName;

    private Integer categoryType;

    private Long parentId;

}

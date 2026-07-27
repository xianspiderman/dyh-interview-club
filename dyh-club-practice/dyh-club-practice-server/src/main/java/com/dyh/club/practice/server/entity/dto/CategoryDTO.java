package com.dyh.club.practice.server.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryDTO {
    //专项练习只刷单选、多选和判断，这是题目的类型的列表
    private List<Integer> subjectTypeList;

    private Integer categoryType;

    private Long parentId;

}

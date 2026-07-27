package com.dyh.club.practice.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpecialPracticeCategoryVO implements Serializable {


    private String categoryName;//分类名称

    private Long categoryId;//分类id

    private List<SpecialPracticeLabelVO> labelList;//标签列表

}

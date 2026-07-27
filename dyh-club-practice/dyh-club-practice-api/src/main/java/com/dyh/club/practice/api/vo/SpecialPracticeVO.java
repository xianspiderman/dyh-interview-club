package com.dyh.club.practice.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpecialPracticeVO implements Serializable {


    private String primaryCategoryName;//大类名称

    private Long primaryCategoryId;//大类岗位id

    private List<SpecialPracticeCategoryVO> categoryList; //大类下分类

}

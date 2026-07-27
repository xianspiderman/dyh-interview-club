package com.dyh.club.subject.application.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目分类
 *
 */
@Data
public class SubjectCategoryDTO implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类类型
     */
    private Integer categoryType;

    /**
     * 图标连接
     */
    private String imageUrl;

    /**
     * 父级id
     */
    private Long parentId;

    /**
     * 数量
     */
    private Integer count;

    /**
     * 标签信息，SubjectCategoryController.java中的queryCategoryAndLabel用，要查分类时把标签也查了
     */
    private List<SubjectLabelDTO> labelDTOList;

}


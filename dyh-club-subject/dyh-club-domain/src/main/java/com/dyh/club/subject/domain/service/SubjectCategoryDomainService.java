package com.dyh.club.subject.domain.service;

import com.dyh.club.subject.domain.entity.SubjectCategoryBO;
import java.util.List;


public interface SubjectCategoryDomainService {

    /**
     * 新增分类
     */
    void add(SubjectCategoryBO subjectCategoryBO);

    /**
     * 查询岗位大类
     */
    List<SubjectCategoryBO> queryCategory(SubjectCategoryBO subjectCategoryBO);

    /**
     * 更新分类
     */
    Boolean update(SubjectCategoryBO subjectCategoryBO);

    /**
     * 删除分类
     */
    Boolean delete(SubjectCategoryBO subjectCategoryBO);

    /**
     * 查询分类及标签
     */
    List<SubjectCategoryBO> queryCategoryAndLabel(SubjectCategoryBO subjectCategoryBO);

}

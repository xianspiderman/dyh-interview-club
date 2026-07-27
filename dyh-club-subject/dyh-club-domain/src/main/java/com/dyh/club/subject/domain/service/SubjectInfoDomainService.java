package com.dyh.club.subject.domain.service;

import com.dyh.club.subject.common.entity.PageResult;
import com.dyh.club.subject.domain.entity.SubjectInfoBO;
import com.dyh.club.subject.domain.entity.SubjectLabelBO;
import com.dyh.club.subject.infra.basic.entity.SubjectInfoEs;

import java.util.List;

/**
 * 题目领域服务
 * 
 */
public interface SubjectInfoDomainService {

    /**
     * 新增题目
     */
    void add(SubjectInfoBO subjectInfoBO);

    /**
     * 分页查询
     */
    PageResult<SubjectInfoBO> getSubjectPage(SubjectInfoBO subjectInfoBO);

    /**
     * 查询题目信息
     */
    SubjectInfoBO querySubjectInfo(SubjectInfoBO subjectInfoBO);

    /**
     * 全文检索
     */
    PageResult<SubjectInfoEs> getSubjectPageBySearch(SubjectInfoBO subjectInfoBO);

    List<SubjectInfoBO> getContributeList();


}


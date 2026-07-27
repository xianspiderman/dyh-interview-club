package com.dyh.club.subject.infra.basic.entity;

import com.dyh.club.subject.common.entity.PageInfo;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class SubjectInfoEs extends PageInfo implements Serializable {

    private Long subjectId;//题目id

    private Long docId;//文档id

    private String subjectName;//题目名称

    private String subjectAnswer;//题目答案

    private String createUser;

    private Long createTime;

    private Integer subjectType;//题目类型

    private String keyWord;//关键词

    private BigDecimal score;//相关分数

}

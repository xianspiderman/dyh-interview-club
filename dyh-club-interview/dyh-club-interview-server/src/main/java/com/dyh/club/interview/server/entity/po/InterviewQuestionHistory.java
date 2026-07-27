package com.dyh.club.interview.server.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 面试题目记录表(InterviewQuestionHistory)实体类
 *
 * @since 2024-05-23 22:56:31
 */
@Data
public class InterviewQuestionHistory implements Serializable {
    private static final long serialVersionUID = -60560874889446691L;
    /**
     * id
     */
    private Long id;
    /**
     * 面试场次ID
     */
    private Long interviewId;
    /**
     * 均分
     */
    private Double score;
    /**
     * 面试关键字
     */
    private String keyWords;
    /**
     * 问题
     */
    private String question;
    /**
     * 答案
     */
    private String answer;
    /**
     * 用户答案
     */
    private String userAnswer;
    /**
     * 创建人
     */
    private String createdBy;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 更新人
     */
    private String updateBy;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 是否被删除 0为删除 1已删除
     */
    private Integer isDeleted;


}


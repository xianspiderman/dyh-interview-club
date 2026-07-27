package com.dyh.club.interview.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 面试题目记录表(InterviewQuestionHistory)实体类
 *
 * @since 2024-05-23 22:56:31
 */
@Data
public class InterviewQuestionHistoryVO implements Serializable {
    private static final long serialVersionUID = -60560874889446691L;

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


}


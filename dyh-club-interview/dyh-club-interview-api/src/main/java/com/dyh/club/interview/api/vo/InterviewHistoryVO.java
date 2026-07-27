package com.dyh.club.interview.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 面试汇总记录表(InterviewHistory)实体类
 *
 * @since 2024-05-23 22:56:03
 */
@Data
public class InterviewHistoryVO implements Serializable {
    private static final long serialVersionUID = -69404155056273562L;
    /**
     * id
     */
    private Long id;
    /**
     * 平均分
     */
    private double avgScore;
    /**
     * 面试关键字
     */
    private String keyWords;
    /**
     * 面试评价
     */
    private String tip;
    /**
     * 创建时间
     */
    private Long createdTime;

}


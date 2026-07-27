package com.dyh.club.interview.server.entity.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 面试汇总记录表(InterviewHistory)实体类
 *
 * @since 2024-05-23 22:56:03
 */
@Data
public class InterviewHistory implements Serializable {
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
     * 简历地址
     */
    private String interviewUrl;
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


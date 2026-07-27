package com.dyh.club.subject.domain.entity;

import com.dyh.club.subject.common.entity.PageInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目点赞消息
 *
 * @since 2024-01-07 23:08:45
 */
@Data
public class SubjectLikedMessage implements Serializable {


    /**
     * 题目id
     */
    private Long subjectId;

    /**
     * 点赞人id
     */
    private String likeUserId;

    /**
     * 点赞状态 1点赞 0不点赞
     */
    private Integer status;

}


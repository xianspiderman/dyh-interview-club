package com.dyh.club.subject.common.enums;

import lombok.Getter;

/**
 * 题目点赞枚举
 *
 */
@Getter
public enum SubjectLikedStatusEnum {

    LIKED(1, "点赞"),
    UN_LIKED(0, "取消点赞");

    public int code;

    public String desc;

    SubjectLikedStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SubjectLikedStatusEnum getByCode(int codeVal) {
        for (SubjectLikedStatusEnum resultCodeEnum : SubjectLikedStatusEnum.values()) {
            if (resultCodeEnum.code == codeVal) {
                return resultCodeEnum;
            }
        }
        return null;
    }

}

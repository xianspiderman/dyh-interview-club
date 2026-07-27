package com.dyh.club.subject.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目答案dto
 *
 */
/**
 * 它代表的是“一个单一的选项”。
 * 想象一道单选题，它有 A、B、C、D 四个选项。
 * A选项：“Java” -> 是否正确：是
 * B选项：“Python” -> 是否正确：否
 * 这里的 SubjectAnswerBO 就是用来精确描述这一行选项数据的。它的三个字段 optionType (A/B/C/D的代号)、optionContent (文本)、isCorrect (对错)，完美且唯一地描述了一个选项的全部属性。
 */
@Data
public class SubjectAnswerBO implements Serializable {

    /**
     * 答案选项标识
     */
    private Integer optionType;

    /**
     * 答案
     */
    private String optionContent;

    /**
     * 是否正确
     */
    private Integer isCorrect;

}


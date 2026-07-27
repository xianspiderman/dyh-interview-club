package com.dyh.club.subject.domain.entity;

import com.dyh.club.subject.common.entity.PageInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目dto
 *
 */

/**
 * 顺着上面的逻辑（dyh-club-subject\dyh-club-domain\src\main\java\com\dyh\subject\domain\entity\SubjectAnswerBO.java），
 * 一道单选题有 A、B、C、D 四个选项。
 * 那么这道题的选项集合，自然就是一个由 4 个 SubjectAnswerBO 组成的列表（List<SubjectAnswerBO> optionList）。
 * 此外，SubjectOptionBO 里面还有一个 subjectAnswer 字段，它是用来兜底的（比如简答题没有选项列表，只有一段纯文本的标准答案，就会存放在这个字段里）。
 */
/*
为什么不把 SubjectOptionBO 里的两个字段全放满？见笔记[37查询题目信息接口开发.md](笔记库\37查询题目信息接口开发.md)
 */
@Data
public class SubjectOptionBO implements Serializable {

    /**
     * 题目答案
     */
    private String subjectAnswer;

    /**
     * 答案选项
     */
    private List<SubjectAnswerBO> optionList;

}


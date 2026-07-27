package com.dyh.club.subject.domain.handler.subject;

import com.dyh.club.subject.common.enums.SubjectInfoTypeEnum;
import com.dyh.club.subject.domain.entity.SubjectInfoBO;
import com.dyh.club.subject.domain.entity.SubjectOptionBO;

public interface SubjectTypeHandler {

    /**
     * 枚举身份的识别
     */
    SubjectInfoTypeEnum getHandlerType();

    /**
     * 实际的题目的插入
     */
    void add(SubjectInfoBO subjectInfoBO);

    /**
     * 查特定题型的选项/答案：调用特定处理器的 query 方法。
     */
    SubjectOptionBO query(int subjectId);

}

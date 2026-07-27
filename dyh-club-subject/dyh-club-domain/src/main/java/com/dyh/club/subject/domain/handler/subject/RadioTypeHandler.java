package com.dyh.club.subject.domain.handler.subject;

import com.dyh.club.subject.common.enums.IsDeletedFlagEnum;
import com.dyh.club.subject.common.enums.SubjectInfoTypeEnum;
import com.dyh.club.subject.domain.convert.MultipleSubjectConverter;
import com.dyh.club.subject.domain.convert.RadioSubjectConverter;
import com.dyh.club.subject.domain.entity.SubjectAnswerBO;
import com.dyh.club.subject.domain.entity.SubjectInfoBO;
import com.dyh.club.subject.domain.entity.SubjectOptionBO;
import com.dyh.club.subject.infra.basic.entity.SubjectMultiple;
import com.dyh.club.subject.infra.basic.entity.SubjectRadio;
import com.dyh.club.subject.infra.basic.service.SubjectRadioService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;

/**
 * 单选题目的策略类
 *
 */
@Component
public class RadioTypeHandler implements SubjectTypeHandler {

    @Resource
    private SubjectRadioService subjectRadioService;

    @Override
    public SubjectInfoTypeEnum getHandlerType() {
        return SubjectInfoTypeEnum.RADIO;
    }

    @Override
    public void add(SubjectInfoBO subjectInfoBO) {
        //单选题目的插入
        List<SubjectRadio> subjectRadioList = new LinkedList<>();
        subjectInfoBO.getOptionList().forEach(option -> {
            SubjectRadio subjectRadio = RadioSubjectConverter.INSTANCE.convertBoToEntity(option);
            subjectRadio.setSubjectId(subjectInfoBO.getId());
            subjectRadio.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
            subjectRadioList.add(subjectRadio);
        });
        subjectRadioService.batchInsert(subjectRadioList);
    }

    @Override
    public SubjectOptionBO query(int subjectId) {
        SubjectRadio subjectRadio = new SubjectRadio();
        subjectRadio.setSubjectId(Long.valueOf(subjectId));
        List<SubjectRadio> result = subjectRadioService.queryByCondition(subjectRadio);
        List<SubjectAnswerBO> subjectAnswerBOList = RadioSubjectConverter.INSTANCE.convertEntityToBoList(result);
        SubjectOptionBO subjectOptionBO = new SubjectOptionBO();
        subjectOptionBO.setOptionList(subjectAnswerBOList);
        return subjectOptionBO;
    }

}

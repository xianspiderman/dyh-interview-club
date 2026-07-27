package com.dyh.club.subject.domain.convert;

import com.dyh.club.subject.domain.entity.SubjectAnswerBO;
import com.dyh.club.subject.infra.basic.entity.SubjectJudge;
import com.dyh.club.subject.infra.basic.entity.SubjectMultiple;
import com.dyh.club.subject.infra.basic.entity.SubjectRadio;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MultipleSubjectConverter {

    MultipleSubjectConverter INSTANCE = Mappers.getMapper(MultipleSubjectConverter.class);

    SubjectMultiple convertBoToEntity(SubjectAnswerBO subjectAnswerBO);

    List<SubjectAnswerBO> convertEntityToBoList(List<SubjectMultiple> subjectMultipleList);


}

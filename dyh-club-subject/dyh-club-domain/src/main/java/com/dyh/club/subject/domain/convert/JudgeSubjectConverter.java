package com.dyh.club.subject.domain.convert;

import com.dyh.club.subject.domain.entity.SubjectAnswerBO;
import com.dyh.club.subject.domain.entity.SubjectInfoBO;
import com.dyh.club.subject.domain.entity.SubjectOptionBO;
import com.dyh.club.subject.infra.basic.entity.SubjectBrief;
import com.dyh.club.subject.infra.basic.entity.SubjectJudge;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface JudgeSubjectConverter {

    JudgeSubjectConverter INSTANCE = Mappers.getMapper(JudgeSubjectConverter.class);

    List<SubjectAnswerBO> convertEntityToBoList(List<SubjectJudge> subjectJudgeList);

}

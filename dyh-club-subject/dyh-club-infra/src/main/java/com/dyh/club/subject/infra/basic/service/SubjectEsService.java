package com.dyh.club.subject.infra.basic.service;

import com.dyh.club.subject.common.entity.PageResult;
import com.dyh.club.subject.infra.basic.entity.SubjectInfoEs;

public interface SubjectEsService {

    boolean insert(SubjectInfoEs subjectInfoEs);

    PageResult<SubjectInfoEs> querySubjectList(SubjectInfoEs subjectInfoEs);

}

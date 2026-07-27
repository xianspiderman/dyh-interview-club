package com.dyh.club.practice.server.dao;

import com.dyh.club.practice.server.entity.po.SubjectJudgePO;

public interface SubjectJudgeDao {


    SubjectJudgePO selectBySubjectId(Long repeatSubjectId);


}
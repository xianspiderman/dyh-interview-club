package com.dyh.club.subject.infra.basic.mapper;

import com.dyh.club.subject.infra.basic.entity.SubjectLiked;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyh.club.subject.infra.basic.entity.SubjectMapping;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 题目点赞表 表数据库访问层
 *
 * @since 2024-01-07 23:08:45
 */
@Repository
public interface SubjectLikedDao extends BaseMapper<SubjectLiked> {

    int insertBatch(@Param("entities") List<SubjectLiked> entities);

    int countByCondition(SubjectLiked subjectLiked);

    List<SubjectLiked> queryPage(@Param("entity") SubjectLiked subjectLiked,
                                 @Param("start") int start,
                                 @Param("pageSize") Integer pageSize);

    void batchInsertOrUpdate(@Param("entities") List<SubjectLiked> subjectLikedList);

}


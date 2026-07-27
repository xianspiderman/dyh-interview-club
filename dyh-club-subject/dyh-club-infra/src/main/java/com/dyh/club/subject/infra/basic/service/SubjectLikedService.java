package com.dyh.club.subject.infra.basic.service;

import com.dyh.club.subject.infra.basic.entity.SubjectLiked;

import java.util.List;

/**
 * 题目点赞表 表服务接口
 *
 * @since 2024-01-07 23:08:45
 */
public interface SubjectLikedService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    SubjectLiked queryById(Long id);

    /**
     * 新增数据
     *
     * @param subjectLiked 实例对象
     * @return 实例对象
     */
    int insert(SubjectLiked subjectLiked);

    /**
     * 修改数据
     *
     * @param subjectLiked 实例对象
     * @return 实例对象
     */
    int update(SubjectLiked subjectLiked);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 根据条件查询角色
     */
    SubjectLiked queryByCondition(SubjectLiked subjectLiked);

    void batchInsert(List<SubjectLiked> subjectLikedList);

    int countByCondition(SubjectLiked subjectLiked);

    List<SubjectLiked> queryPage(SubjectLiked subjectLiked, int start, Integer pageSize);

    void batchInsertOrUpdate(List<SubjectLiked> subjectLikedList);

}

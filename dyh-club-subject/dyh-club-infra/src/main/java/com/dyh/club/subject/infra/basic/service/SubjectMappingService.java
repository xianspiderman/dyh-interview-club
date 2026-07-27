package com.dyh.club.subject.infra.basic.service;

import com.dyh.club.subject.infra.basic.entity.SubjectMapping;

import java.util.List;

/**
 * 题目分类关系表(SubjectMapping)表服务接口
 *
 * @since 2023-10-03 22:17:07
 */
public interface SubjectMappingService {

    /**
     * 通过ID查询单条数据
     *
     * @return 实例对象
     */
    SubjectMapping queryById(int id);

    /**
     * 新增数据
     *
     * @param subjectMapping 实例对象
     * @return 实例对象
     */
    SubjectMapping insert(SubjectMapping subjectMapping);

    /**
     * 修改数据
     *
     * @param subjectMapping 实例对象
     * @return 实例对象
     */
    int update(SubjectMapping subjectMapping);

    /**
     * 通过主键删除数据
     *
     * @return 是否成功
     */
    boolean deleteById(int id);

    /**
     * 查询标签id
     */
    List<SubjectMapping> queryLabelId(SubjectMapping subjectMapping);

    /**
     * 批量插入
     */
    void batchInsert(List<SubjectMapping> mappingList);

}

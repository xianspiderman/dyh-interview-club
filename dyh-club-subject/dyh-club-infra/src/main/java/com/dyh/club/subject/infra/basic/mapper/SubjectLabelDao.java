package com.dyh.club.subject.infra.basic.mapper;

import com.dyh.club.subject.infra.basic.entity.SubjectLabel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目标签表(SubjectLabel)表数据库访问层
 *
 * @since 2023-10-03 21:50:29
 */
public interface SubjectLabelDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    SubjectLabel queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param subjectLabel 查询条件
     * @return 对象列表
     */
    List<SubjectLabel> queryByCondition(SubjectLabel subjectLabel);

    /**
     * 统计总行数
     *
     * @param subjectLabel 查询条件
     * @return 总行数
     */
    long count(SubjectLabel subjectLabel);

    /**
     * 新增数据
     *
     * @param subjectLabel 实例对象
     * @return 影响行数
     */
    int insert(SubjectLabel subjectLabel);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<SubjectLabel> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SubjectLabel> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<SubjectLabel> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<SubjectLabel> entities);

    /**
     * 修改数据
     *
     * @param subjectLabel 实例对象
     * @return 影响行数
     */
    int update(SubjectLabel subjectLabel);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

    List<SubjectLabel> batchQueryById(@Param("list") List<Long> labelIdList);

}


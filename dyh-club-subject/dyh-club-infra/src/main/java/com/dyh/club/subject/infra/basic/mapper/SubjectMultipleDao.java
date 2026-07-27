package com.dyh.club.subject.infra.basic.mapper;

import com.dyh.club.subject.infra.basic.entity.SubjectMultiple;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 多选题信息表(SubjectMultiple)表数据库访问层
 *
 * @since 2023-10-05 21:30:05
 */
public interface SubjectMultipleDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    SubjectMultiple queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param subjectMultiple 查询条件
     * @return 对象列表
     */
    List<SubjectMultiple> queryAllByLimit(SubjectMultiple subjectMultiple);

    /**
     * 统计总行数
     *
     * @param subjectMultiple 查询条件
     * @return 总行数
     */
    long count(SubjectMultiple subjectMultiple);

    /**
     * 新增数据
     *
     * @param subjectMultiple 实例对象
     * @return 影响行数
     */
    int insert(SubjectMultiple subjectMultiple);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<SubjectMultiple> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SubjectMultiple> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<SubjectMultiple> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<SubjectMultiple> entities);

    /**
     * 修改数据
     *
     * @param subjectMultiple 实例对象
     * @return 影响行数
     */
    int update(SubjectMultiple subjectMultiple);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}


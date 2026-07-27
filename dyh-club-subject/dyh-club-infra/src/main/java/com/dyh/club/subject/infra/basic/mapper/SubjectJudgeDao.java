package com.dyh.club.subject.infra.basic.mapper;

import com.dyh.club.subject.infra.basic.entity.SubjectJudge;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 判断题(SubjectJudge)表数据库访问层
 *
 * @since 2023-10-05 21:29:47
 */
public interface SubjectJudgeDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    SubjectJudge queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param subjectJudge 查询条件
     * @return 对象列表
     */
    List<SubjectJudge> queryAllByLimit(SubjectJudge subjectJudge);

    /**
     * 统计总行数
     *
     * @param subjectJudge 查询条件
     * @return 总行数
     */
    long count(SubjectJudge subjectJudge);

    /**
     * 新增数据
     *
     * @param subjectJudge 实例对象
     * @return 影响行数
     */
    int insert(SubjectJudge subjectJudge);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<SubjectJudge> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SubjectJudge> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<SubjectJudge> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<SubjectJudge> entities);

    /**
     * 修改数据
     *
     * @param subjectJudge 实例对象
     * @return 影响行数
     */
    int update(SubjectJudge subjectJudge);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}


package com.dyh.club.interview.server.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyh.club.interview.server.entity.po.InterviewQuestionHistory;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 面试题目记录表(InterviewQuestionHistory)表数据库访问层
 *
 * @since 2024-05-23 22:56:31
 */
public interface InterviewQuestionHistoryDao extends BaseMapper<InterviewQuestionHistory> {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    InterviewQuestionHistory queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param interviewQuestionHistory 查询条件
     * @param pageable                 分页对象
     * @return 对象列表
     */
    List<InterviewQuestionHistory> queryAllByLimit(InterviewQuestionHistory interviewQuestionHistory, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param interviewQuestionHistory 查询条件
     * @return 总行数
     */
    long count(InterviewQuestionHistory interviewQuestionHistory);

    /**
     * 新增数据
     *
     * @param interviewQuestionHistory 实例对象
     * @return 影响行数
     */
    int insert(InterviewQuestionHistory interviewQuestionHistory);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<InterviewQuestionHistory> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<InterviewQuestionHistory> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<InterviewQuestionHistory> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<InterviewQuestionHistory> entities);

    /**
     * 修改数据
     *
     * @param interviewQuestionHistory 实例对象
     * @return 影响行数
     */
    int update(InterviewQuestionHistory interviewQuestionHistory);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}


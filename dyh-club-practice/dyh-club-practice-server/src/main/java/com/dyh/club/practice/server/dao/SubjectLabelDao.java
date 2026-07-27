package com.dyh.club.practice.server.dao;

import com.dyh.club.practice.server.entity.po.SubjectLabelPO;
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
    SubjectLabelPO queryById(Long id);

    /**
     * 批量查询当前题目的标签名称
     */
    List<String> getLabelNameByIds(@Param("labelIds") List<Long> labelIds);


}


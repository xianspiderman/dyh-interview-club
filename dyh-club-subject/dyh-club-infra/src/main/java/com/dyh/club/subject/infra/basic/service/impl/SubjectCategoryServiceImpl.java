package com.dyh.club.subject.infra.basic.service.impl;

import com.alibaba.fastjson.JSON;
import com.dyh.club.subject.infra.basic.entity.SubjectCategory;
import com.dyh.club.subject.infra.basic.mapper.SubjectCategoryDao;
import com.dyh.club.subject.infra.basic.service.SubjectCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 题目分类(SubjectCategory)表服务实现类
 *
 * @since 2023-10-01 21:50:05
 */
@Service("subjectCategoryService")
@Slf4j
public class SubjectCategoryServiceImpl implements SubjectCategoryService {

    @Resource
    private SubjectCategoryDao subjectCategoryDao;


    /**
     * 新增数据
     *
     * @param subjectCategory 实例对象
     * @return 实例对象
     */
    @Override
    public SubjectCategory insert(SubjectCategory subjectCategory) {
        if(log.isInfoEnabled()){
            log.info("SubjectCategoryController.add.subjectCategory:{}"
                    , JSON.toJSONString(subjectCategory));
        }
        this.subjectCategoryDao.insert(subjectCategory);
        return subjectCategory;
    }

    @Override
    public SubjectCategory queryById(Long id) {
        return this.subjectCategoryDao.queryById(id);
    }

    /**
     * 修改数据
     *
     * @param subjectCategory 实例对象
     * @return 实例对象
     */
    @Override
    public int update(SubjectCategory subjectCategory) {
        return this.subjectCategoryDao.update(subjectCategory);
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.subjectCategoryDao.deleteById(id) > 0;
    }

    @Override
    public List<SubjectCategory> queryCategory(SubjectCategory subjectCategory) {
        return this.subjectCategoryDao.queryCategory(subjectCategory);
    }

    @Override
    public Integer querySubjectCount(Long id) {
        return this.subjectCategoryDao.querySubjectCount(id);
    }
}

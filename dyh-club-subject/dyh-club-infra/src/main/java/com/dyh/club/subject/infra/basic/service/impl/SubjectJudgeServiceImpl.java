package com.dyh.club.subject.infra.basic.service.impl;

import com.dyh.club.subject.infra.basic.entity.SubjectJudge;
import com.dyh.club.subject.infra.basic.mapper.SubjectJudgeDao;
import com.dyh.club.subject.infra.basic.service.SubjectJudgeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 判断题(SubjectJudge)表服务实现类
 *
 * @since 2023-10-05 21:29:47
 */
@Service("subjectJudgeService")
public class SubjectJudgeServiceImpl implements SubjectJudgeService {
    @Resource
    private SubjectJudgeDao subjectJudgeDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public SubjectJudge queryById(Long id) {
        return this.subjectJudgeDao.queryById(id);
    }

    /**
     * 新增数据
     *
     * @param subjectJudge 实例对象
     * @return 实例对象
     */
    @Override
    public SubjectJudge insert(SubjectJudge subjectJudge) {
        this.subjectJudgeDao.insert(subjectJudge);
        return subjectJudge;
    }

    /**
     * 修改数据
     *
     * @param subjectJudge 实例对象
     * @return 实例对象
     */
    @Override
    public SubjectJudge update(SubjectJudge subjectJudge) {
        this.subjectJudgeDao.update(subjectJudge);
        return this.queryById(subjectJudge.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.subjectJudgeDao.deleteById(id) > 0;
    }

    @Override
    public List<SubjectJudge> queryByCondition(SubjectJudge subjectJudge) {
        return this.subjectJudgeDao.queryAllByLimit(subjectJudge);
    }
}

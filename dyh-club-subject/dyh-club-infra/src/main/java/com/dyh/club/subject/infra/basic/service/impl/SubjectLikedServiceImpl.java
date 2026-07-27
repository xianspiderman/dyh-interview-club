package com.dyh.club.subject.infra.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dyh.club.subject.infra.basic.entity.SubjectLiked;
import com.dyh.club.subject.infra.basic.entity.SubjectMapping;
import com.dyh.club.subject.infra.basic.mapper.SubjectLikedDao;
import com.dyh.club.subject.infra.basic.service.SubjectLikedService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 题目点赞表 表服务实现类
 *
 * @since 2024-01-07 23:08:45
 */
@Service("SubjectLikedService")
public class SubjectLikedServiceImpl implements SubjectLikedService {

    @Resource
    private SubjectLikedDao subjectLikedDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public SubjectLiked queryById(Long id) {
        return this.subjectLikedDao.selectById(id);
    }

    /**
     * 新增数据
     *
     * @param subjectLiked 实例对象
     * @return 实例对象
     */
    @Override
    public int insert(SubjectLiked subjectLiked) {
        return this.subjectLikedDao.insert(subjectLiked);
    }

    /**
     * 修改数据
     *
     * @param subjectLiked 实例对象
     * @return 实例对象
     */
    @Override
    public int update(SubjectLiked subjectLiked) {
        return this.subjectLikedDao.updateById(subjectLiked);
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.subjectLikedDao.deleteById(id) > 0;
    }

    /**
     * 条件查询
     *
     * @param subjectLiked 条件
     * @return 实例对象
     */
    @Override
    public SubjectLiked queryByCondition(SubjectLiked subjectLiked) {

        LambdaQueryWrapper<SubjectLiked> queryWrapper = Wrappers.<SubjectLiked>lambdaQuery()
                .eq(Objects.nonNull(subjectLiked.getId()), SubjectLiked::getId, subjectLiked.getId())
                .eq(Objects.nonNull(subjectLiked.getSubjectId()), SubjectLiked::getSubjectId, subjectLiked.getSubjectId())
                .eq(Objects.nonNull(subjectLiked.getLikeUserId()), SubjectLiked::getLikeUserId, subjectLiked.getLikeUserId())
                .eq(Objects.nonNull(subjectLiked.getStatus()), SubjectLiked::getStatus, subjectLiked.getStatus())
                .eq(Objects.nonNull(subjectLiked.getCreatedBy()), SubjectLiked::getCreatedBy, subjectLiked.getCreatedBy())
                .eq(Objects.nonNull(subjectLiked.getCreatedTime()), SubjectLiked::getCreatedTime, subjectLiked.getCreatedTime())
                .eq(Objects.nonNull(subjectLiked.getUpdateBy()), SubjectLiked::getUpdateBy, subjectLiked.getUpdateBy())
                .eq(Objects.nonNull(subjectLiked.getUpdateTime()), SubjectLiked::getUpdateTime, subjectLiked.getUpdateTime())
                .eq(Objects.nonNull(subjectLiked.getIsDeleted()), SubjectLiked::getIsDeleted, subjectLiked.getIsDeleted());
        return subjectLikedDao.selectOne(queryWrapper);

    }

    @Override
    public void batchInsert(List<SubjectLiked> subjectLikedList) {
        this.subjectLikedDao.insertBatch(subjectLikedList);
    }

    @Override
    public int countByCondition(SubjectLiked subjectLiked) {
        return this.subjectLikedDao.countByCondition(subjectLiked);
    }

    @Override
    public List<SubjectLiked> queryPage(SubjectLiked subjectLiked, int start, Integer pageSize) {
        return this.subjectLikedDao.queryPage(subjectLiked, start, pageSize);
    }

    @Override
    public void batchInsertOrUpdate(List<SubjectLiked> subjectLikedList) {
        this.subjectLikedDao.batchInsertOrUpdate(subjectLikedList);
    }

}

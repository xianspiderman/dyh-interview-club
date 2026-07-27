package com.dyh.club.subject.domain.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyh.club.subject.common.entity.PageResult;
import com.dyh.club.subject.common.enums.IsDeletedFlagEnum;
import com.dyh.club.subject.common.util.IdWorkerUtil;
import com.dyh.club.subject.common.util.LoginUtil;
import com.dyh.club.subject.domain.convert.SubjectCategoryConverter;
import com.dyh.club.subject.domain.convert.SubjectInfoConverter;
import com.dyh.club.subject.domain.entity.SubjectCategoryBO;
import com.dyh.club.subject.domain.entity.SubjectInfoBO;
import com.dyh.club.subject.domain.entity.SubjectOptionBO;
import com.dyh.club.subject.domain.handler.subject.SubjectTypeHandler;
import com.dyh.club.subject.domain.handler.subject.SubjectTypeHandlerFactory;
import com.dyh.club.subject.domain.redis.RedisUtil;
import com.dyh.club.subject.domain.service.SubjectCategoryDomainService;
import com.dyh.club.subject.domain.service.SubjectInfoDomainService;
import com.dyh.club.subject.domain.service.SubjectLikedDomainService;
import com.dyh.club.subject.infra.basic.entity.*;
import com.dyh.club.subject.infra.basic.service.*;
import com.dyh.club.subject.infra.entity.UserInfo;
import com.dyh.club.subject.infra.rpc.UserRpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubjectInfoDomainServiceImpl implements SubjectInfoDomainService {

    @Resource
    private SubjectInfoService subjectInfoService;

    @Resource
    private SubjectMappingService subjectMappingService;

    @Resource
    private SubjectLabelService subjectLabelService;

    @Resource
    private SubjectTypeHandlerFactory subjectTypeHandlerFactory;

    @Resource
    private SubjectEsService subjectEsService;

    @Resource
    private SubjectLikedDomainService subjectLikedDomainService;

    @Resource
    private UserRpc userRpc;

    @Resource
    private RedisUtil redisUtil;

    private static final String RANK_KEY = "subject_rank";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SubjectInfoBO subjectInfoBO) {
        if (log.isInfoEnabled()) {
            log.info("SubjectInfoDomainServiceImpl.add.bo:{}", JSON.toJSONString(subjectInfoBO));
        }
        SubjectInfo subjectInfo = SubjectInfoConverter.INSTANCE.convertBoToInfo(subjectInfoBO);
        subjectInfo.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        subjectInfoService.insert(subjectInfo);
        SubjectTypeHandler handler = subjectTypeHandlerFactory.getHandler(subjectInfo.getSubjectType());
        subjectInfoBO.setId(subjectInfo.getId());
        handler.add(subjectInfoBO);

        List<Integer> categoryIds = subjectInfoBO.getCategoryIds();
        List<Integer> labelIds = subjectInfoBO.getLabelIds();
        List<SubjectMapping> mappingList = new LinkedList<>();// 因为要在subject_mapping插入，找到infra层的SubjectMapping的类
        categoryIds.forEach(categoryId -> {
            labelIds.forEach(labelId -> {
                SubjectMapping subjectMapping = new SubjectMapping();
                subjectMapping.setSubjectId(subjectInfo.getId());
                subjectMapping.setCategoryId(Long.valueOf(categoryId));//categoryId是lang类型的
                subjectMapping.setLabelId(Long.valueOf(labelId));
                subjectMapping.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
                mappingList.add(subjectMapping);
            });
        });
        subjectMappingService.batchInsert(mappingList);//正式插入到subject_mapping表，需要引入SubjectMappingService
        //同步到es，com/dyh/subject/infra/basic/service/impl/SubjectEsServiceImpl.java的insert函数
        SubjectInfoEs subjectInfoEs = new SubjectInfoEs();
        subjectInfoEs.setDocId(new IdWorkerUtil(1, 1, 1).nextId());//会用就行
        subjectInfoEs.setSubjectId(subjectInfo.getId());
        subjectInfoEs.setSubjectAnswer(subjectInfoBO.getSubjectAnswer());
        subjectInfoEs.setCreateTime(new Date().getTime());
        subjectInfoEs.setCreateUser("DYH Club");
        subjectInfoEs.setSubjectName(subjectInfo.getSubjectName());
        subjectInfoEs.setSubjectType(subjectInfo.getSubjectType());
        subjectEsService.insert(subjectInfoEs);

        redisUtil.addScore(RANK_KEY, LoginUtil.getLoginId(), 1);//1是分数加1，来一个就在LoginId的分数加一

    }

    @Override
    // 查询题目列表
    public PageResult<SubjectInfoBO> getSubjectPage(SubjectInfoBO subjectInfoBO) {
        PageResult<SubjectInfoBO> pageResult = new PageResult<>();
        pageResult.setPageNo(subjectInfoBO.getPageNo());
        pageResult.setPageSize(subjectInfoBO.getPageSize());
        int start = (subjectInfoBO.getPageNo() - 1) * subjectInfoBO.getPageSize();//计算开始页
        SubjectInfo subjectInfo = SubjectInfoConverter.INSTANCE.convertBoToInfo(subjectInfoBO);
        int count = subjectInfoService.countByCondition(subjectInfo, subjectInfoBO.getCategoryId()//计算一共多少数据
                , subjectInfoBO.getLabelId());// 因为subjectDifficult题目难度在subjectInfo里，subjectInfoBO没有这个变量
        if (count == 0) {
            return pageResult;
        }
        List<SubjectInfo> subjectInfoList = subjectInfoService.queryPage(subjectInfo, subjectInfoBO.getCategoryId()
                , subjectInfoBO.getLabelId(), start, subjectInfoBO.getPageSize());
        List<SubjectInfoBO> subjectInfoBOS = SubjectInfoConverter.INSTANCE.convertListInfoToBO(subjectInfoList);
        subjectInfoBOS.forEach(info -> {
            SubjectMapping subjectMapping = new SubjectMapping();
            subjectMapping.setSubjectId(info.getId());
            List<SubjectMapping> mappingList = subjectMappingService.queryLabelId(subjectMapping);
            List<Long> labelIds = mappingList.stream().map(SubjectMapping::getLabelId).collect(Collectors.toList());
            List<SubjectLabel> labelList = subjectLabelService.batchQueryById(labelIds);
            List<String> labelNames = labelList.stream().map(SubjectLabel::getLabelName).collect(Collectors.toList());
            info.setLabelName(labelNames);
        });
        pageResult.setRecords(subjectInfoBOS);
        pageResult.setTotal(count);
        return pageResult;
    }

    @Override
    // 查询题目信息
    public SubjectInfoBO querySubjectInfo(SubjectInfoBO subjectInfoBO) {
        // 1. 查基础信息：利用传入的 ID，去 subject_info 主表查询题目的基础信息（名称、难度、类型、分数等）。
        // 注意：这里查出来的 subjectInfo 里面是没有 ABCD 选项和具体答案的，因为它们存在别的表里。
        SubjectInfo subjectInfo = subjectInfoService.queryById(subjectInfoBO.getId());
        // 2. 策略模式获取处理器：这是一个非常高级的工程设计（工厂模式+策略模式）。
        // 根据题目的类型（subjectType，比如1代表单选，4代表简答），从工厂中获取对应的处理器（Handler）。
        // 比如：如果是单选题，拿到的就是 RadioTypeHandler；简答题拿到的就是 BriefTypeHandler。
        SubjectTypeHandler handler = subjectTypeHandlerFactory.getHandler(subjectInfo.getSubjectType());
        // 3. 查特定题型的选项/答案：调用特定处理器的query方法。
        // 如果是单选题处理器，它底层就会去subject_radio表查ABCD选项；
        // 查询的结果统一被包装成了SubjectOptionBO（里面包含了选项列表和答案）。
        SubjectOptionBO optionBO = handler.query(subjectInfo.getId().intValue());
        // 4. 对象合并（拼图第一步）：利用MapStruct转换工具，把“主表的基础信息(subjectInfo)” 和 “从表的选项信息(optionBO)” 合并。
        // 塞进一个新的、庞大的 SubjectInfoBO 对象（bo）中。此时 bo 已经有了题目名字、分数、以及 ABCD 选项了。
        SubjectInfoBO bo = SubjectInfoConverter.INSTANCE.convertOptionAndInfoToBo(optionBO, subjectInfo);

        SubjectMapping subjectMapping = new SubjectMapping();
        subjectMapping.setSubjectId(subjectInfo.getId());
        subjectMapping.setIsDeleted(IsDeletedFlagEnum.UN_DELETED.getCode());
        // 8. 查映射表：去 subject_mapping 表里查出这道题关联的所有映射记录。
        List<SubjectMapping> mappingList = subjectMappingService.queryLabelId(subjectMapping);
        // 9. 提取标签 ID：利用 Java 8 的 Stream 流，把 mappingList 里面每个对象的 LabelId 抽出来，组成一个纯净的 List<Long>。
        List<Long> labelIdList = mappingList.stream().map(SubjectMapping::getLabelId).collect(Collectors.toList());
        // 10. 查标签详情：拿着一堆标签 ID（比如 [10, 15]），去 subject_label 表里做批量查询（IN 查询），得到完整的标签实体列表。
        List<SubjectLabel> labelList = subjectLabelService.batchQueryById(labelIdList);
        // 11. 提取标签名称：再次利用 Stream 流，只把标签的名称（比如 "Java并发", "锁"）抽出来，变成 List<String>。
        List<String> labelNameList = labelList.stream().map(SubjectLabel::getLabelName).collect(Collectors.toList());
        // 12. 拼装标签（拼图第二步）：把标签名字列表塞进刚刚合并好的大对象 bo 中。
        bo.setLabelName(labelNameList);
        bo.setLiked(subjectLikedDomainService.isLiked(subjectInfoBO.getId().toString(), LoginUtil.getLoginId()));//是否点赞
        bo.setLikedCount(subjectLikedDomainService.getLikedCount(subjectInfoBO.getId().toString()));
        assembleSubjectCursor(subjectInfoBO, bo);
        return bo;// 13. 大功告成：返回包含所有完整信息的 SubjectInfoBO。
    }

    private void assembleSubjectCursor(SubjectInfoBO subjectInfoBO, SubjectInfoBO bo) {

        Long categoryId = subjectInfoBO.getCategoryId();
        Long labelId = subjectInfoBO.getLabelId();
        Long subjectId = subjectInfoBO.getId();//题目id，别忘了id是自增的
        if (Objects.isNull(categoryId) || Objects.isNull(labelId)) {
            return;
        }
        Long nextSubjectId = subjectInfoService.querySubjectIdCursor(subjectId, categoryId, labelId, 1);//1是传下一题，查subject_info和subject_mapping表
        bo.setNextSubjectId(nextSubjectId);
        Long lastSubjectId = subjectInfoService.querySubjectIdCursor(subjectId, categoryId, labelId, 0);
        bo.setLastSubjectId(lastSubjectId);
    }

    @Override//查询数据，检索
    public PageResult<SubjectInfoEs> getSubjectPageBySearch(SubjectInfoBO subjectInfoBO) {
        SubjectInfoEs subjectInfoEs = new SubjectInfoEs();
        subjectInfoEs.setPageNo(subjectInfoBO.getPageNo());
        subjectInfoEs.setPageSize(subjectInfoBO.getPageSize());
        subjectInfoEs.setKeyWord(subjectInfoBO.getKeyWord());
        return subjectEsService.querySubjectList(subjectInfoEs);
    }

    @Override
    public List<SubjectInfoBO> getContributeList() {

        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisUtil.rankWithScore(RANK_KEY, 0, 5);
        if (log.isInfoEnabled()) {
            log.info("getContributeList.typedTuples:{}", JSON.toJSONString(typedTuples));
        }
        if (CollectionUtils.isEmpty(typedTuples)) {
            return Collections.emptyList();
        }
        List<SubjectInfoBO> boList = new LinkedList<>();
        typedTuples.forEach((rank -> {
            SubjectInfoBO subjectInfoBO = new SubjectInfoBO();
            subjectInfoBO.setSubjectCount(rank.getScore().intValue());
            UserInfo userInfo = userRpc.getUserInfo(rank.getValue());//Value就是LoginId
            subjectInfoBO.setCreateUser(userInfo.getNickName());
            subjectInfoBO.setCreateUserAvatar(userInfo.getAvatar());
            boList.add(subjectInfoBO);
        }));
        return boList;
    }

}

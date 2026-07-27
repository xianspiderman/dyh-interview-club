package com.dyh.club.subject.infra.basic.service.impl;

import com.dyh.club.subject.common.entity.PageResult;
import com.dyh.club.subject.common.enums.SubjectInfoTypeEnum;
import com.dyh.club.subject.infra.basic.entity.EsSubjectFields;
import com.dyh.club.subject.infra.basic.entity.SubjectInfoEs;
import com.dyh.club.subject.infra.basic.es.EsIndexInfo;
import com.dyh.club.subject.infra.basic.es.EsRestClient;
import com.dyh.club.subject.infra.basic.es.EsSearchRequest;
import com.dyh.club.subject.infra.basic.es.EsSourceData;
import com.dyh.club.subject.infra.basic.service.SubjectEsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class SubjectEsServiceImpl implements SubjectEsService {

    @Override //插入数据，在com/dyh/subject/domain/service/impl/SubjectInfoDomainServiceImpl.java里面的add(SubjectInfoBO subjectInfoBO)插入题目同时同步到es
    public boolean insert(SubjectInfoEs subjectInfoEs) {
        EsSourceData esSourceData = new EsSourceData();
        Map<String, Object> data = convert2EsSourceData(subjectInfoEs);
        esSourceData.setDocId(subjectInfoEs.getDocId().toString());
        esSourceData.setData(data);
        return EsRestClient.insertDoc(getEsIndexInfo(), esSourceData);
    }

    private Map<String, Object> convert2EsSourceData(SubjectInfoEs subjectInfoEs) {
        Map<String, Object> data = new HashMap<>();
        data.put(EsSubjectFields.SUBJECT_ID, subjectInfoEs.getSubjectId());
        data.put(EsSubjectFields.DOC_ID, subjectInfoEs.getDocId());
        data.put(EsSubjectFields.SUBJECT_NAME, subjectInfoEs.getSubjectName());
        data.put(EsSubjectFields.SUBJECT_ANSWER, subjectInfoEs.getSubjectAnswer());
        data.put(EsSubjectFields.SUBJECT_TYPE, subjectInfoEs.getSubjectType());
        data.put(EsSubjectFields.CREATE_USER, subjectInfoEs.getCreateUser());
        data.put(EsSubjectFields.CREATE_TIME, subjectInfoEs.getCreateTime());
        return data;
    }

    @Override//查询数据，检索
    // 在com/dyh/subject/application/controller/SubjectController.java中的全文检索getSubjectPageBySearch函数使用
    // 在com/dyh/subject/domain/service/impl/SubjectInfoDomainServiceImpl.java的getSubjectPageBySearch函数使用
    public PageResult<SubjectInfoEs> querySubjectList(SubjectInfoEs req) {
        PageResult<SubjectInfoEs> pageResult = new PageResult<>();
        EsSearchRequest esSearchRequest = createSearchListQuery(req);//帮我把SubjectInfoEs req转成EsSearchRequest
        SearchResponse searchResponse = EsRestClient.searchWithTermQuery(getEsIndexInfo(), esSearchRequest);

        List<SubjectInfoEs> subjectInfoEsList = new LinkedList<>();
        SearchHits searchHits = searchResponse.getHits();//拿到命中的数据
        if (searchHits == null || searchHits.getHits() == null) {
            pageResult.setPageNo(req.getPageNo());
            pageResult.setPageSize(req.getPageSize());
            pageResult.setRecords(subjectInfoEsList);
            pageResult.setTotal(0);
            return pageResult;//没命中返回
        }
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            SubjectInfoEs subjectInfoEs = convertResult(hit);
            if (Objects.nonNull(subjectInfoEs)) {
                subjectInfoEsList.add(subjectInfoEs);
            }
        }
        pageResult.setPageNo(req.getPageNo());
        pageResult.setPageSize(req.getPageSize());
        pageResult.setRecords(subjectInfoEsList);
        pageResult.setTotal(Long.valueOf(searchHits.getTotalHits().value).intValue());
        return pageResult;
    }

    private SubjectInfoEs convertResult(SearchHit hit) {
        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
        if (CollectionUtils.isEmpty(sourceAsMap)) {
            return null;
        }
        SubjectInfoEs result = new SubjectInfoEs();
        result.setSubjectId(MapUtils.getLong(sourceAsMap, EsSubjectFields.SUBJECT_ID));
        result.setSubjectName(MapUtils.getString(sourceAsMap, EsSubjectFields.SUBJECT_NAME));

        result.setSubjectAnswer(MapUtils.getString(sourceAsMap, EsSubjectFields.SUBJECT_ANSWER));

        result.setDocId(MapUtils.getLong(sourceAsMap, EsSubjectFields.DOC_ID));
        result.setSubjectType(MapUtils.getInteger(sourceAsMap, EsSubjectFields.SUBJECT_TYPE));
        result.setScore(new BigDecimal(String.valueOf(hit.getScore())).multiply(new BigDecimal("100.00")
                .setScale(2, RoundingMode.HALF_UP)));//设置分数，%制度，2位小数，向上取整

        //处理name的高亮
        Map<String, HighlightField> highlightFields = hit.getHighlightFields();//得到高亮字段
        HighlightField subjectNameField = highlightFields.get(EsSubjectFields.SUBJECT_NAME);
        if(Objects.nonNull(subjectNameField)){//看看题目是否命中
            Text[] fragments = subjectNameField.getFragments();
            StringBuilder subjectNameBuilder = new StringBuilder();
            for (Text fragment : fragments) {
                subjectNameBuilder.append(fragment);
            }
            result.setSubjectName(subjectNameBuilder.toString());
        }

        //处理答案高亮
        HighlightField subjectAnswerField = highlightFields.get(EsSubjectFields.SUBJECT_ANSWER);
        if(Objects.nonNull(subjectAnswerField)){
            Text[] fragments = subjectAnswerField.getFragments();
            StringBuilder subjectAnswerBuilder = new StringBuilder();
            for (Text fragment : fragments) {
                subjectAnswerBuilder.append(fragment);
            }
            result.setSubjectAnswer(subjectAnswerBuilder.toString());
        }

        return result;
    }

    private EsSearchRequest createSearchListQuery(SubjectInfoEs req) {
        EsSearchRequest esSearchRequest = new EsSearchRequest();
        BoolQueryBuilder bq = new BoolQueryBuilder();
        MatchQueryBuilder subjectNameQueryBuilder =
                QueryBuilders.matchQuery(EsSubjectFields.SUBJECT_NAME, req.getKeyWord());//用户的输入的KeyWord应该和SUBJECT_NAME进行匹配

        bq.should(subjectNameQueryBuilder); //bq应该去匹配题目名称
        subjectNameQueryBuilder.boost(2);//提高匹配题目的权重

        MatchQueryBuilder subjectAnswerQueryBuilder =
                QueryBuilders.matchQuery(EsSubjectFields.SUBJECT_ANSWER, req.getKeyWord());
        bq.should(subjectAnswerQueryBuilder);//bq应该去匹配题目答案

        MatchQueryBuilder subjectTypeQueryBuilder =
                QueryBuilders.matchQuery(EsSubjectFields.SUBJECT_TYPE, SubjectInfoTypeEnum.BRIEF.getCode());
        bq.must(subjectTypeQueryBuilder);//bq必须是匹配的题目简答题（项目设置只匹配简答）
        bq.minimumShouldMatch(1);//最少命中一个
        //前置搜索搞定，开始设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder().field("*").requireFieldMatch(false);
        highlightBuilder.preTags("<span style = \"color:red\">");//设置红色前缀
        highlightBuilder.postTags("</span>");//后缀
        //开始组装esSearchRequest
        esSearchRequest.setBq(bq);
        esSearchRequest.setHighlightBuilder(highlightBuilder);
        esSearchRequest.setFields(EsSubjectFields.FIELD_QUERY);//设置匹配的字段数组
        esSearchRequest.setFrom((req.getPageNo() - 1) * req.getPageSize());//固定算法
        esSearchRequest.setSize(req.getPageSize());//固定算法
        esSearchRequest.setNeedScroll(false);
        return esSearchRequest;
    }

    private EsIndexInfo getEsIndexInfo() {
        EsIndexInfo esIndexInfo = new EsIndexInfo();
        esIndexInfo.setClusterName("73438a827b55");
        esIndexInfo.setIndexName("subject_index");
        return esIndexInfo;
    }
}

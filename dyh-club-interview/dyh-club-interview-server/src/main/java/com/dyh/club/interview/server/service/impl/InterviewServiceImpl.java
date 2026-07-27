package com.dyh.club.interview.server.service.impl;

import com.google.common.base.Preconditions;
import com.dyh.club.interview.api.req.InterviewReq;
import com.dyh.club.interview.api.req.InterviewSubmitReq;
import com.dyh.club.interview.api.req.StartReq;
import com.dyh.club.interview.api.vo.InterviewQuestionVO;
import com.dyh.club.interview.api.vo.InterviewResultVO;
import com.dyh.club.interview.api.vo.InterviewVO;
import com.dyh.club.interview.server.dao.SubjectDao;
import com.dyh.club.interview.server.entity.po.SubjectLabel;
import com.dyh.club.interview.server.service.InterviewEngine;
import com.dyh.club.interview.server.service.InterviewService;
import com.dyh.club.interview.server.util.PDFUtil;
import com.dyh.club.interview.server.util.keyword.KeyWordUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterviewServiceImpl implements InterviewService, ApplicationContextAware {

    private static final Map<String, InterviewEngine> engineMap = new HashMap<>();

    @Resource
    private SubjectDao subjectLabelDao;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Collection<InterviewEngine> engines = applicationContext.getBeansOfType(InterviewEngine.class).values();
        for (InterviewEngine engine : engines) {
            engineMap.put(engine.engineType().name(), engine);
        }
    }

    @Override
    public InterviewVO analyse(InterviewReq req) {
        List<String> keyWords = buildKeyWords(req.getUrl());
        InterviewEngine engine = engineMap.get(req.getEngine());
        Preconditions.checkArgument(!Objects.isNull(engine), "引擎不能为空！");
        return engine.analyse(keyWords);
    }

    @Override
    public InterviewQuestionVO start(StartReq req) {
        InterviewEngine engine = engineMap.get(req.getEngine());
        Preconditions.checkArgument(!Objects.isNull(engine), "引擎不能为空！");
        return engine.start(req);
    }


    @Override
    public InterviewResultVO submit(InterviewSubmitReq req) {
        InterviewEngine engine = engineMap.get(req.getEngine());
        Preconditions.checkArgument(!Objects.isNull(engine), "引擎不能为空！");
        return engine.submit(req);
    }

    private List<String> buildKeyWords(String url) {
        String pdfText = PDFUtil.getPdfText(url);
        if (!KeyWordUtil.isInit()) {
            List<String> list = subjectLabelDao.listAllLabel().stream().map(SubjectLabel::getLabelName).collect(Collectors.toList());
            KeyWordUtil.addWord(list);
        }
        return KeyWordUtil.buildKeyWordsLists(pdfText);
    }

}

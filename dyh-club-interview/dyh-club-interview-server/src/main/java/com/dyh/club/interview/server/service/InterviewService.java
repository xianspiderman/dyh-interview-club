package com.dyh.club.interview.server.service;

import com.dyh.club.interview.api.req.InterviewReq;
import com.dyh.club.interview.api.req.InterviewSubmitReq;
import com.dyh.club.interview.api.req.StartReq;
import com.dyh.club.interview.api.vo.InterviewQuestionVO;
import com.dyh.club.interview.api.vo.InterviewResultVO;
import com.dyh.club.interview.api.vo.InterviewVO;

public interface InterviewService {

    InterviewVO analyse(InterviewReq req);

    InterviewQuestionVO start(StartReq req);

    InterviewResultVO submit(InterviewSubmitReq req);
}

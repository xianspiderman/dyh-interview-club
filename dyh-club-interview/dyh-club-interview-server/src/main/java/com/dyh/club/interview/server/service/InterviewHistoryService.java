package com.dyh.club.interview.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dyh.club.interview.api.common.PageResult;
import com.dyh.club.interview.api.req.InterviewHistoryReq;
import com.dyh.club.interview.api.req.InterviewSubmitReq;
import com.dyh.club.interview.api.vo.InterviewHistoryVO;
import com.dyh.club.interview.api.vo.InterviewResultVO;
import com.dyh.club.interview.server.entity.po.InterviewHistory;

/**
 * 面试汇总记录表(InterviewHistory)表服务接口
 *
 * @since 2024-05-23 22:56:03
 */
public interface InterviewHistoryService extends IService<InterviewHistory> {

    void logInterview(InterviewSubmitReq req, InterviewResultVO submit);


    PageResult<InterviewHistoryVO> getHistory(InterviewHistoryReq req);

}

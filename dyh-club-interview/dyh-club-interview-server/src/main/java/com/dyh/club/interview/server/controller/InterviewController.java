package com.dyh.club.interview.server.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.dyh.club.auth.entity.Result;
import com.dyh.club.interview.api.common.PageResult;
import com.dyh.club.interview.api.req.InterviewHistoryReq;
import com.dyh.club.interview.api.req.InterviewReq;
import com.dyh.club.interview.api.req.InterviewSubmitReq;
import com.dyh.club.interview.api.req.StartReq;
import com.dyh.club.interview.api.vo.*;
import com.dyh.club.interview.server.service.InterviewHistoryService;
import com.dyh.club.interview.server.service.InterviewQuestionHistoryService;
import com.dyh.club.interview.server.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 模拟面试信息 前端控制器
 * </p>
 *
 * @since 2024/05/16
 */
@Slf4j
@RestController
@RequestMapping("/interview")
public class InterviewController {

    @Resource
    private InterviewService interviewService;

    @Resource
    private InterviewHistoryService interviewHistoryService;

    @Resource
    private InterviewQuestionHistoryService interviewQuestionHistoryService;

    /**
     * 分析简历
     */
    @PostMapping(value = "/analyse")
    public Result<InterviewVO> analyse(@RequestBody InterviewReq req) {
        try {
            if (log.isInfoEnabled()) {
                log.info("分析简历入参{}", JSON.toJSON(req));
            }
            Preconditions.checkArgument(!Objects.isNull(req), "参数不能为空！");
            Preconditions.checkArgument(!Objects.isNull(req.getEngine()), "引擎不能为空！");
            Preconditions.checkArgument(!Objects.isNull(req.getUrl()), "简历不能为空！");
            return Result.ok(interviewService.analyse(req));
        } catch (IllegalArgumentException e) {
            log.error("参数异常！错误原因{}", e.getMessage(), e);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("分析简历异常！错误原因{}", e.getMessage(), e);
            return Result.fail("分析简历异常！");
        }
    }

    /**
     * 开始面试
     */
    @PostMapping(value = "/start")
    public Result<InterviewQuestionVO> start(@RequestBody StartReq req) {
        try {
            if (log.isInfoEnabled()) {
                log.info("开始面试入参{}", JSON.toJSON(req));
            }
            Preconditions.checkArgument(!Objects.isNull(req), "参数不能为空！");
            Preconditions.checkArgument(!Objects.isNull(req.getEngine()), "引擎不能为空！");
            Preconditions.checkArgument(!Objects.isNull(req.getQuestionList()), "关键字不能为空！");
            return Result.ok(interviewService.start(req));
        } catch (IllegalArgumentException e) {
            log.error("参数异常！错误原因{}", e.getMessage(), e);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("开始面试异常！错误原因{}", e.getMessage(), e);
            return Result.fail("开始面试异常！");
        }
    }

    /**
     * 面试提交答案
     */
    @PostMapping(value = "/submit")
    public Result<InterviewResultVO> submit(@RequestBody InterviewSubmitReq req) {
        try {
            if (log.isInfoEnabled()) {
                log.info("面试提交入参{}", JSON.toJSON(req));
            }
            Preconditions.checkArgument(!Objects.isNull(req), "参数不能为空！");
            InterviewResultVO submit = interviewService.submit(req);
            interviewHistoryService.logInterview(req, submit);
            return Result.ok(submit);
        } catch (IllegalArgumentException e) {
            log.error("参数异常！错误原因{}", e.getMessage(), e);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("分析简历异常！错误原因{}", e.getMessage(), e);
            return Result.fail("分析简历异常！");
        }
    }

    /**
     * 分页查询面试记录
     */
    @PostMapping(value = "/getHistory")
    public Result<PageResult<InterviewHistoryVO>> getHistory(@RequestBody InterviewHistoryReq req) {
        try {
            if (log.isInfoEnabled()) {
                log.info("分页查询面试记录入参{}", JSON.toJSONString(req));
            }
            Preconditions.checkArgument(!Objects.isNull(req), "参数不能为空！");
            PageResult<InterviewHistoryVO> result = interviewHistoryService.getHistory(req);
            if (log.isInfoEnabled()) {
                log.info("分页查询面试记录出参{}", JSON.toJSONString(result));
            }
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("参数异常！错误原因{}", e.getMessage(), e);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("分页查询面试记录异常！错误原因{}", e.getMessage(), e);
            return Result.fail("分页查询面试记录异常！");
        }
    }

    /**
     * 查询详情
     */
    @GetMapping(value = "/detail")
    public Result<List<InterviewQuestionHistoryVO>> detail(Long id) {
        try {
            if (log.isInfoEnabled()) {
                log.info("查询详情入参{}", id);
            }
            Preconditions.checkArgument(!Objects.isNull(id), "参数不能为空！");
            List<InterviewQuestionHistoryVO> result = interviewQuestionHistoryService.detail(id);
            if (log.isInfoEnabled()) {
                log.info("查询详情出参{}", JSON.toJSONString(result));
            }
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("参数异常！错误原因{}", e.getMessage(), e);
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询详情异常！错误原因{}", e.getMessage(), e);
            return Result.fail("查询详情异常！");
        }
    }


}

package com.dyh.club.runtime.practice;
import com.dyh.club.platform.common.BizException;import org.springframework.stereotype.Component;import java.util.Map;
@Component public class SubjectQuestionFallback implements SubjectQuestionClient{
 public Map<String,Object>detail(long id,String token){throw new BizException(503,"题目服务暂不可用，请稍后重试");}
 public Map<String,Boolean>correct(long id,String answer,String token){throw new BizException(503,"题目服务暂不可用，暂不能交卷");}
}

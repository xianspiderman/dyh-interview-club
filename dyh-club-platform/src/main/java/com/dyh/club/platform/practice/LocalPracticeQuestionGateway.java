package com.dyh.club.platform.practice;

import com.dyh.club.platform.question.QuestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix="club.practice",name="remote-subject",havingValue="false",matchIfMissing=true)
public class LocalPracticeQuestionGateway implements PracticeQuestionGateway {
    private final QuestionService questions;
    public LocalPracticeQuestionGateway(QuestionService questions){this.questions=questions;}
    public Map<String,Object> detail(long questionId){return questions.detail(questionId,false);}
    public boolean isCorrect(long questionId,String answer){return questions.isCorrect(questionId,answer);}
}

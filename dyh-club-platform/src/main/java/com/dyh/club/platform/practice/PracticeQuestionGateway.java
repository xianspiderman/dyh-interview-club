package com.dyh.club.platform.practice;

import java.util.Map;

public interface PracticeQuestionGateway {
    Map<String,Object> detail(long questionId);
    boolean isCorrect(long questionId,String answer);
}

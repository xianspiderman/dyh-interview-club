package com.dyh.club.platform.question;

import java.util.Map;

public interface QuestionTypeHandler {
    String type();
    void save(long questionId, QuestionModels.QuestionRequest request);
    void delete(long questionId);
    Map<String, Object> load(long questionId);
    boolean isCorrect(long questionId, String answer);
}

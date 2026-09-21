package com.dyh.club.runtime.practice;

import com.dyh.club.platform.practice.PracticeQuestionGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RemotePracticeQuestionGateway implements PracticeQuestionGateway {
    private final SubjectQuestionClient client;
    private final String token;

    public RemotePracticeQuestionGateway(SubjectQuestionClient client,
                                         @Value("${club.internal.token}") String token) {
        if (token == null || token.trim().length() < 16) {
            throw new IllegalStateException("CLUB_INTERNAL_TOKEN 必须由环境变量提供且不少于 16 个字符");
        }
        this.client = client;
        this.token = token;
    }

    public Map<String, Object> detail(long id) {
        return client.detail(id, token);
    }

    public boolean isCorrect(long id, String answer) {
        return Boolean.TRUE.equals(client.correct(id, answer, token).get("correct"));
    }
}

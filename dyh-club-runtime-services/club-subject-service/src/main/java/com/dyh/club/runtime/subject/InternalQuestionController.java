package com.dyh.club.runtime.subject;

import com.dyh.club.platform.question.QuestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/internal/questions")
public class InternalQuestionController {
    private final QuestionService questions;
    private final byte[] token;

    public InternalQuestionController(QuestionService questions,
                                      @Value("${club.internal.token}") String token) {
        if (token == null || token.trim().length() < 16) {
            throw new IllegalStateException("CLUB_INTERNAL_TOKEN 必须由环境变量提供且不少于 16 个字符");
        }
        this.questions = questions;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    private void check(String supplied) {
        byte[] candidate = supplied == null ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(token, candidate)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部调用凭证无效");
        }
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id,
                                      @RequestHeader("X-Club-Internal") String supplied) {
        check(supplied);
        return questions.detail(id, false);
    }

    @GetMapping("/{id}/correct")
    public Map<String, Boolean> correct(@PathVariable long id, @RequestParam String answer,
                                        @RequestHeader("X-Club-Internal") String supplied) {
        check(supplied);
        return Collections.singletonMap("correct", questions.isCorrect(id, answer));
    }
}

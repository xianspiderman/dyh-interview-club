package com.dyh.club.platform.question;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Arrays;
import java.util.Collections;

@Component
@Order(2)
@ConditionalOnProperty(prefix="club.demo",name="enabled",havingValue="true")
public class DemoQuestionInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final QuestionService questions;

    public DemoQuestionInitializer(JdbcTemplate jdbc, QuestionService questions) { this.jdbc = jdbc; this.questions = questions; }

    @Override public void run(String... args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM club_question", Integer.class);
        if (count != null && count > 0) return;
        Long admin = jdbc.queryForObject("SELECT id FROM club_user WHERE username='club-admin'", Long.class);
        QuestionModels.QuestionRequest radio = new QuestionModels.QuestionRequest();
        radio.name = "HashMap 在 Java 8 中何时由链表转为红黑树？"; radio.analysis = "达到树化阈值且数组容量满足要求时才会树化。";
        radio.type = "RADIO"; radio.status = "PUBLISHED"; radio.categoryIds = Collections.singletonList(2L); radio.labelIds = Collections.singletonList(1L);
        radio.options = Arrays.asList(option("A", "链表长度达到 8 且数组容量至少为 64", true), option("B", "链表长度达到 4", false), option("C", "每次扩容都会树化", false));
        questions.create(radio, admin);

        QuestionModels.QuestionRequest multiple = new QuestionModels.QuestionRequest();
        multiple.name = "下列哪些机制可以减少缓存不一致风险？"; multiple.analysis = "删除缓存、失效通知和过期时间分别覆盖不同失败窗口。";
        multiple.type = "MULTIPLE"; multiple.status = "PUBLISHED"; multiple.categoryIds = Collections.singletonList(3L); multiple.labelIds = Collections.singletonList(3L);
        multiple.options = Arrays.asList(option("A", "先更新数据库再删除缓存", true), option("B", "Redis Pub/Sub 通知本地缓存失效", true), option("C", "缓存永不过期", false));
        questions.create(multiple, admin);

        QuestionModels.QuestionRequest brief = new QuestionModels.QuestionRequest();
        brief.name = "说明 ThreadLocal 在线程池中的风险和治理方式"; brief.analysis = "关注线程复用、跨线程传递和 finally 清理。";
        brief.type = "BRIEF"; brief.status = "PUBLISHED"; brief.categoryIds = Collections.singletonList(3L); brief.labelIds = Collections.singletonList(3L);
        brief.referenceAnswer = "线程池复用线程会保留未清理的数据；异步任务不会自动继承上下文，应提交时显式复制，并在 finally 中恢复或清理。";
        questions.create(brief, admin);
    }

    private QuestionModels.OptionRequest option(String code, String content, boolean correct) {
        QuestionModels.OptionRequest option = new QuestionModels.OptionRequest(); option.code = code; option.content = content; option.correct = correct; return option;
    }
}

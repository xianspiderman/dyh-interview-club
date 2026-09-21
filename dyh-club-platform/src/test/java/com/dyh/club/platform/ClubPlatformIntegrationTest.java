package com.dyh.club.platform;

import com.dyh.club.platform.community.CommunityService;
import com.dyh.club.platform.community.SensitiveWordService;
import com.dyh.club.platform.like.*;
import com.dyh.club.platform.practice.PracticeService;
import com.dyh.club.platform.question.QuestionService;
import com.dyh.club.platform.question.QuestionModels;
import com.dyh.club.platform.search.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClubPlatformIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired QuestionService questions;
    @Autowired PracticeService practices;
    @Autowired LikePersistenceService likePersistence;
    @Autowired LikeService likes;
    @Autowired CommunityService community;
    @Autowired SensitiveWordService sensitive;
    @Autowired SearchService search;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test @Order(1)
    void migrationsAndDemoDataAreAvailable() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class)).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_question", Integer.class)).isGreaterThanOrEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_user", Integer.class)).isEqualTo(2);
    }

    @Test @Order(2)
    void anonymousDetailMustNotPolluteCachedAnswers() {
        long id=jdbc.queryForObject("SELECT MIN(id) FROM club_question WHERE question_type='RADIO'",Long.class);
        Map<String,Object> anonymous=questions.detail(id,false);
        assertThat((List<?>)anonymous.get("options")).allSatisfy(option->assertThat((Map<String,Object>)option).doesNotContainKey("correct"));
        Map<String,Object> authenticated=questions.detail(id,true);
        assertThat((List<?>)authenticated.get("options")).anySatisfy(option->assertThat((Map<String,Object>)option).containsKey("correct"));
    }

    @Test @Order(3)
    void practiceProgressAndConditionalSubmitAreIdempotent() {
        long userId=jdbc.queryForObject("SELECT id FROM club_user WHERE username='club-user'",Long.class);
        PracticeService.GenerateRequest request=new PracticeService.GenerateRequest();request.count=2;request.title="集成测试专项";
        long id=practices.generate(userId,request);
        Map<String,Object> resumed=practices.resume(userId,id);
        @SuppressWarnings("unchecked") List<Map<String,Object>> items=(List<Map<String,Object>>)resumed.get("questions");
        long questionId=((Number)items.get(0).get("questionId")).longValue();
        String correct=jdbc.queryForObject("SELECT GROUP_CONCAT(option_code ORDER BY option_code SEPARATOR ',') FROM club_question_option WHERE question_id=? AND correct_flag=TRUE",String.class,questionId);
        practices.saveAnswer(userId,id,questionId,correct,42);
        Map<String,Object> first=practices.submit(userId,id,60),second=practices.submit(userId,id,99);
        assertThat(first.get("correct_count")).isEqualTo(second.get("correct_count"));
        assertThat(jdbc.queryForObject("SELECT version_no FROM club_practice WHERE id=?",Long.class,id)).isEqualTo(1L);
        assertThat(second.get("total_count")).isEqualTo(2);
    }

    @Test @Order(4)
    void likeConsumerIgnoresDuplicateAndOutOfOrderEvents() {
        long q=jdbc.queryForObject("SELECT MIN(id) FROM club_question",Long.class),u=jdbc.queryForObject("SELECT id FROM club_user WHERE username='club-user'",Long.class);
        assertThat(likePersistence.apply(new LikeEvent(q,u,true,2))).isTrue();
        assertThat(likePersistence.apply(new LikeEvent(q,u,false,1))).isFalse();
        assertThat(likePersistence.apply(new LikeEvent(q,u,true,2))).isFalse();
        assertThat(jdbc.queryForObject("SELECT liked FROM club_question_like WHERE question_id=? AND user_id=?",Boolean.class,q,u)).isTrue();
    }

    @Test @Order(5)
    void commentsSupportRootPreviewAndLazyReplies() {
        long userId=jdbc.queryForObject("SELECT id FROM club_user WHERE username='club-user'",Long.class);
        CommunityService.PostRequest post=new CommunityService.PostRequest();post.circleId=jdbc.queryForObject("SELECT MIN(id) FROM club_circle",Long.class);post.title="JVM 学习记录";post.content="记录一次真实的排查过程";
        long postId=community.createPost(userId,post);
        CommunityService.CommentRequest root=new CommunityService.CommentRequest();root.bizType="POST";root.bizId=postId;root.content="第一条评论";long rootId=community.createComment(userId,root);
        for(int i=0;i<5;i++){CommunityService.CommentRequest reply=new CommunityService.CommentRequest();reply.bizType="POST";reply.bizId=postId;reply.parentId=rootId;reply.content="回复 "+i;community.createComment(userId,reply);}
        CommunityService.CommentQuery query=new CommunityService.CommentQuery();query.bizType="POST";query.bizId=postId;
        @SuppressWarnings("unchecked") List<Map<String,Object>> roots=(List<Map<String,Object>>)community.comments(query).get("records");
        assertThat((List<?>)roots.get(0).get("replies")).hasSize(3);assertThat(roots.get(0).get("replyTotal")).isEqualTo(5L);
        assertThat((List<?>)community.replies(rootId,1,20).get("records")).hasSize(5);
    }

    @Test @Order(6)
    void sensitiveDictionaryHotReloadAndInterferenceRemovalWork() {
        sensitive.saveWord("危险词","BLACK");
        assertThatThrownBy(()->sensitive.assertAllowed("危-险_词")).hasMessageContaining("不合规");
        sensitive.saveWord("非危险词","WHITE");
        assertThatCode(()->sensitive.assertAllowed("非危险词")).doesNotThrowAnyException();
    }

    @Test @Order(7)
    void searchFallsBackToRestrictedMysql() {
        Map<String,Object> result=search.search("Java",null,null,null,"relevance",1,10);
        assertThat(result).containsEntry("source","MYSQL_LIMITED").containsEntry("degraded",true);
        assertThatThrownBy(()->search.search("",null,null,null,"relevance",6,10)).hasMessageContaining("前 5 页");
    }

    @Test @Order(8)
    void loginPermissionAndDashboardWorkEndToEnd() throws Exception {
        String body=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"club-admin\",\"password\":\"Club@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.role").value("ADMIN")).andReturn().getResponse().getContentAsString();
        JsonNode json=mapper.readTree(body);String token=json.at("/data/tokenValue").asText();
        mvc.perform(get("/api/admin/overview").header("satoken",token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.questions").isNumber());
        mvc.perform(get("/api/ops/dashboard")).andExpect(status().isOk()).andExpect(jsonPath("$.data.database.status").value("UP"));
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }

    @Test @Order(9)
    void registrationLoginAndLogoutFormACompleteSessionLifecycle() throws Exception {
        String username="integration-user";
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"username\":\""+username+"\",\"password\":\"StrongPass123\",\"nickname\":\"集成测试用户\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").isNumber());
        String body=mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\""+username+"\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=mapper.readTree(body).at("/data/tokenValue").asText();
        mvc.perform(get("/api/auth/me").header("satoken",token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("集成测试用户"));
        mvc.perform(post("/api/auth/logout").header("satoken",token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("satoken",token)).andExpect(status().isUnauthorized());
    }

    @Test @Order(10)
    void questionCrudStatusAndCacheInvalidationAreConnected() {
        long category=jdbc.queryForObject("SELECT MIN(id) FROM club_category WHERE parent_id IS NOT NULL",Long.class);
        long label=jdbc.queryForObject("SELECT MIN(id) FROM club_label WHERE category_id=?",Long.class,category);
        long adminId=jdbc.queryForObject("SELECT id FROM club_user WHERE username='club-admin'",Long.class);
        QuestionModels.QuestionRequest request=new QuestionModels.QuestionRequest();request.name="缓存一致性测试题";request.analysis="第一版解析";request.type="RADIO";request.status="DRAFT";request.categoryIds=Collections.singletonList(category);request.labelIds=Collections.singletonList(label);
        QuestionModels.OptionRequest yes=new QuestionModels.OptionRequest();yes.code="A";yes.content="正确";yes.correct=true;
        QuestionModels.OptionRequest no=new QuestionModels.OptionRequest();no.code="B";no.content="错误";request.options=Arrays.asList(yes,no);
        long id=questions.create(request,adminId);assertThat(questions.detail(id,true)).containsEntry("status","DRAFT");
        request.name="缓存一致性测试题（已编辑）";request.analysis="第二版解析";questions.update(id,request);
        questions.changeStatus(id,"PUBLISHED");assertThat(questions.detail(id,true)).containsEntry("name",request.name).containsEntry("status","PUBLISHED");
        questions.changeStatus(id,"OFFLINE");assertThat(questions.detail(id,true)).containsEntry("status","OFFLINE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM club_cache_invalidation_task WHERE cache_key=?",Integer.class,"club:question:detail:"+id)).isGreaterThanOrEqualTo(4);
    }

    @Test @Order(11)
    void likeApiFallbackKeepsStateAndControlledRecoveryIsBounded() {
        long q=jdbc.queryForObject("SELECT MAX(id) FROM club_question WHERE status='PUBLISHED'",Long.class),u=jdbc.queryForObject("SELECT id FROM club_user WHERE username='club-user'",Long.class);
        assertThat(likes.set(q,u,true)).containsEntry("liked",true);
        assertThat(likes.status(q,u)).containsEntry("liked",true).containsEntry("count",1L);
        assertThat(likes.set(q,u,false)).containsEntry("liked",false);
        assertThat(likes.status(q,u)).containsEntry("liked",false).containsEntry("count",0L);
        assertThat(likes.recover(1000)).isZero();
    }
}

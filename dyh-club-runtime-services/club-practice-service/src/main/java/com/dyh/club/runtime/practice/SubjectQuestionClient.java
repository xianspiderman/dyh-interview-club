package com.dyh.club.runtime.practice;
import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;import java.util.Map;
@FeignClient(name="subjectQuestionClient",url="${club.practice.subject-url:http://localhost:3022}",fallback=SubjectQuestionFallback.class)
public interface SubjectQuestionClient{
 @GetMapping("/internal/questions/{id}")Map<String,Object>detail(@PathVariable("id")long id,@RequestHeader("X-Club-Internal")String token);
 @GetMapping("/internal/questions/{id}/correct")Map<String,Boolean>correct(@PathVariable("id")long id,@RequestParam("answer")String answer,@RequestHeader("X-Club-Internal")String token);
}

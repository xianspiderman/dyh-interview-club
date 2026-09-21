package com.dyh.club.gateway.filter;
import org.junit.jupiter.api.Test;import org.springframework.mock.http.server.reactive.MockServerHttpRequest;import static org.assertj.core.api.Assertions.assertThat;
class RateLimitRuleTest{
 @Test void fixedInterviewRulesAreKeptExactly(){
  assertRule("/api/auth/login",20,5,5,60000,false);
  assertRule("/api/search",200,10,20,1000,true);
  assertRule("/api/practices/9/submit",30,1,2,1000,false);
  assertRule("/api/questions/9/like",100,2,4,1000,false);
 }
 @Test void bothLayersUseExpiringRedisTokenBucketLua(){assertThat(RateLimitFilter.TOKEN_BUCKET_SCRIPT).contains("hmget","tokens","last","pexpire","tokens>=1");}
 private void assertRule(String path,long global,long rate,long capacity,long period,boolean failOpen){RateLimitFilter.Rule r=RateLimitFilter.rule(MockServerHttpRequest.post(path).build());assertThat(r).isNotNull();assertThat(r.globalLimit).isEqualTo(global);assertThat(r.userRate).isEqualTo(rate);assertThat(r.userCapacity).isEqualTo(capacity);assertThat(r.userPeriodMillis).isEqualTo(period);assertThat(r.failOpen).isEqualTo(failOpen);}
}

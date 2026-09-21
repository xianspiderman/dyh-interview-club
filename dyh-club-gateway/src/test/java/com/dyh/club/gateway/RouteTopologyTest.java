package com.dyh.club.gateway;
import org.junit.jupiter.api.Test;import java.nio.charset.StandardCharsets;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class RouteTopologyTest{
 @Test void apiMainChainUsesFourCoreServicesWithoutPlatformCatchAll()throws Exception{
  String yaml=new String(Files.readAllBytes(Paths.get("src/main/resources/application.yml")),StandardCharsets.UTF_8);
  assertThat(yaml).contains("club-auth-service","club-subject-service","club-practice-service","club-circle-service");
  assertThat(yaml).doesNotContain("id: club-platform","CLUB_PLATFORM_URI","Path=/api/**,/actuator/**");
  assertThat(yaml.indexOf("club-circle-sensitive-admin")).isLessThan(yaml.indexOf("club-subject-service"));
  assertThat(yaml.indexOf("club-circle-content-admin")).isLessThan(yaml.indexOf("club-subject-service"));
 }
}

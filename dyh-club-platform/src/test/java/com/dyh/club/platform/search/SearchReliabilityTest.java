package com.dyh.club.platform.search;

import com.dyh.club.lock.DistributedLock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SearchReliabilityTest {
    @Test
    void keepsEveryConfiguredClusterNodeAndIkMapping() {
        SearchService service = new SearchService(mock(JdbcTemplate.class), new ObjectMapper(), false,
                "http://es01:9200,http://es02:9200,http://es03:9200", "club-subject-search");

        assertThat(service.configuredNodeCount()).isEqualTo(3);
        assertThat(new ObjectMapper().valueToTree(SearchService.indexDefinition()).toString())
                .contains("ik_max_word", "ik_smart", "answer", "number_of_replicas\":1");
    }

    @Test
    void mappingContainsNameKeywordAndCreatedAtDate() {
        JsonNode properties = new ObjectMapper().valueToTree(SearchService.indexDefinition())
                .path("mappings").path("properties");

        assertThat(properties.path("name").path("fields").path("keyword").path("type").asText())
                .isEqualTo("keyword");
        assertThat(properties.path("createdAt").path("type").asText()).isEqualTo("date");
        assertThat(properties.path("createdAt").path("format").asText())
                .isEqualTo("strict_date_optional_time");
    }

    @Test
    void relevanceQueryUsesTwoToOneWeightIdTieBreakAndOnlyNameAnswerHighlight() {
        JsonNode body = new ObjectMapper().valueToTree(
                SearchService.searchRequestBody("并发", 2L, 3L, "radio", "relevance", 1, 20));

        JsonNode fields = body.path("query").path("bool").path("must").path("multi_match").path("fields");
        assertThat(Arrays.asList(fields.get(0).asText(), fields.get(1).asText()))
                .containsExactly("name^2", "answer");
        assertThat(body.path("sort").get(0).path("_score").asText()).isEqualTo("desc");
        assertThat(body.path("sort").get(1).path("id").asText()).isEqualTo("desc");
        Set<String> highlightFields = new LinkedHashSet<>();
        body.path("highlight").path("fields").fieldNames().forEachRemaining(highlightFields::add);
        assertThat(highlightFields).containsExactlyInAnyOrder("name", "answer");
        assertThat(body.toString()).doesNotContain("analysis");
    }

    @Test
    void bulkChecksEveryItemAndRetriesOnlyFailures() {
        Set<Long> ids = new LinkedHashSet<>(Arrays.asList(11L, 12L, 13L));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", Arrays.asList(item("index", 201), item("index", 429), item("delete", 404)));

        assertThat(SearchService.failedIds(ids, response)).containsExactly(12L);
    }

    @Test
    void rejectsCanalBatchLargerThanFiveHundred() {
        SearchService service = new SearchService(mock(JdbcTemplate.class), new ObjectMapper(), false,
                "http://es01:9200", "a");
        Set<Long> ids = new LinkedHashSet<>();
        for (long i = 1; i <= 501; i++) {
            ids.add(i);
        }

        assertThatThrownBy(() -> service.bulkIndex("a", ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void rebuildAndCanalUseTheSameDistributedCutoverLock() throws Exception {
        DistributedLock rebuild = SearchService.class.getMethod("rebuild")
                .getAnnotation(DistributedLock.class);
        DistributedLock canal = SearchService.class.getMethod("indexCanalBatch", Set.class)
                .getAnnotation(DistributedLock.class);

        assertThat(rebuild).isNotNull();
        assertThat(canal).isNotNull();
        assertThat(rebuild.prefix()).isEqualTo("search:cutover");
        assertThat(canal.prefix()).isEqualTo(rebuild.prefix());
        assertThat(canal.key()).isEqualTo(rebuild.key());
    }

    private Map<String, Object> item(String action, int status) {
        return Collections.singletonMap(action, Collections.singletonMap("status", status));
    }
}

package com.example.livescore.service;

import com.example.livescore.model.ScoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EventScoreFetcherTest {

    RestTemplate template;
    MockRestServiceServer mock;
    EventScoreFetcher fetcher;

    @BeforeEach
    void setUp() {
        template = new RestTemplate();
        mock = MockRestServiceServer.bindTo(template).build();
        fetcher = new EventScoreFetcher(template, "http://external-service:8081/api/events/");
    }

    @Test
    void returnsScore() {
        mock.expect(once(), requestTo("http://external-service:8081/api/events/E1/score"))
                .andRespond(withSuccess("""
                            {"eventId":"E1","currentScore":"1:0"}
                        """, MediaType.APPLICATION_JSON));

        var score = fetcher.fetch("E1");
        assertThat(score).isEqualTo(new ScoreResponse("E1", "1:0"));
    }
}

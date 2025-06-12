package com.example.livescore.integration;

import com.example.livescore.model.EventStatus;
import com.example.livescore.store.EventStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;

import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        // point producer at embedded broker
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        // faster scheduler
        "live-score.polling-ms=500",
        // stub base URL
        "live-score.external-base-url=http://stub/api/events/"
})
@EmbeddedKafka(partitions = 1, topics = "live-scores")
class LiveScoreSchedulerIT {

    @Autowired
    EventStore store;
    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    RestTemplate restTemplate;

    @Test
    void schedulerPublishesToKafka() {

        /*  Stub external REST endpoint */
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.once(),
                        requestTo("http://stub/api/events/GAME42/score"))
                .andRespond(withSuccess("""
                            {"eventId":"GAME42","currentScore":"1:0"}
                        """, MediaType.APPLICATION_JSON));

        /*  Mark event live (controller not needed) */
        store.setStatus("GAME42", EventStatus.LIVE);

        /*  Prepare consumer */
        Map<String, Object> props = KafkaTestUtils.consumerProps("itGroup", "false", broker);
        try (var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(
                props, new StringDeserializer(), new StringDeserializer())) {

            consumer.subscribe(List.of("live-scores"));

            /*  Wait for scheduler → fetch → publish  */
            ConsumerRecord<String, String> rec =
                    KafkaTestUtils.getSingleRecord(consumer, "live-scores", Duration.ofSeconds(5));

            assertThat(rec.value()).contains("\"eventId\":\"GAME42\"")
                    .contains("\"currentScore\":\"1:0\"");
        }

        /*  Verify stub was hit exactly once */
        server.verify();
    }
}

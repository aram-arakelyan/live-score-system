package com.example.livescore.integration;

import com.example.livescore.model.EventStatus;
import com.example.livescore.store.EventStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "live-score.polling-ms=500",
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
    void schedulerPublishesContinuously() {

        /* stub external API  */
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(ExpectedCount.manyTimes(),
                        requestTo("http://stub/api/events/GAME42/score"))
                .andRespond(withSuccess("""
                            {"eventId":"GAME42","currentScore":"1:0"}
                        """, MediaType.APPLICATION_JSON));

        /* event live */
        store.setStatus("GAME42", EventStatus.LIVE);

        /* consumer */
        Map<String, Object> props = KafkaTestUtils.consumerProps("itGroup", "false", broker);
        try (var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(
                props, new StringDeserializer(), new StringDeserializer())) {

            consumer.subscribe(List.of("live-scores"));

            // expect at least two records within 3 seconds ( 500 ms poll cycle )
            Iterable<ConsumerRecord<String, String>> iterable =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(3))
                            .records("live-scores");

            List<ConsumerRecord<String, String>> list = new ArrayList<>();
            iterable.forEach(list::add);

            assertThat(list).hasSizeGreaterThanOrEqualTo(2);
            assertThat(list.get(0).value()).contains("\"eventId\":\"GAME42\"");
        }

        server.verify();
    }
}

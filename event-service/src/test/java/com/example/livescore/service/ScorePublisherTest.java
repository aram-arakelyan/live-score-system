package com.example.livescore.service;

import com.example.livescore.model.ScoreResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class ScorePublisherTest {

    @Test
    void serialisesAndSends() throws Exception {
        // 1️⃣  Mock template and stub a proper future
        KafkaTemplate<String, String> kt = mock(KafkaTemplate.class);

        SendResult<String, String> dummy = new SendResult<>(
                new ProducerRecord<>("live-scores", "payload"), null);

        when(kt.send(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummy));

        // 2️⃣  Create publisher and inject topic
        ScorePublisher pub = new ScorePublisher(new ObjectMapper(), kt);
        ReflectionTestUtils.setField(pub, "topic", "live-scores");   // <-- key line

        // 3️⃣  Call method under test
        pub.publish("E1", new ScoreResponse("E1", "0:0"));

        // 4️⃣  Verify interaction
        verify(kt).send(eq("live-scores"), contains("\"eventId\":\"E1\""));
    }

}

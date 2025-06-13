package com.example.livescore.service;

import com.example.livescore.model.ScoreResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScorePublisherTest {

    @Test
    void serialisesAndSends() throws Exception {
        // Mock template
        KafkaTemplate<String, String> kt = mock(KafkaTemplate.class);

        SendResult<String, String> dummy =
                new SendResult<>(new ProducerRecord<>("live-scores", "payload"), null);
        when(kt.send(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummy));

        // Publisher instance
        ScorePublisher pub = new ScorePublisher(new ObjectMapper(), kt);
        // inject topic
        org.springframework.test.util.ReflectionTestUtils
                .setField(pub, "topic", "live-scores");

        // Call
        pub.publish("E1", new ScoreResponse("E1", "0:0"));

        // Verify
        verify(kt).send(eq("live-scores"), contains("\"eventId\":\"E1\""));
    }
}

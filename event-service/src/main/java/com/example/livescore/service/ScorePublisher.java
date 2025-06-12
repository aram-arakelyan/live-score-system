package com.example.livescore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScorePublisher {

    @Value("${score-publisher.topic}")
    private String topic;

    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafka;

    public void publish(String eventId, Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            kafka.send(topic, json)
                    .whenComplete((meta, ex) -> {
                        if (ex != null) {
                            log.error("Kafka send failed for {}: {}", eventId, ex.getMessage());
                        } else {
                            long offset = meta.getRecordMetadata().offset();
                            log.info("Sent score for {} to Kafka (offset {})", eventId, offset);
                        }
                    });
        } catch (JsonProcessingException ex) {
            log.error("JSON serialisation error for {}: {}", eventId, ex.getMessage());
        }
    }
}

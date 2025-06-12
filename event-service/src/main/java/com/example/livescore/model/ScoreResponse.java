package com.example.livescore.model;

/**
 * Response from external API & outbound Kafka message.
 */
public record ScoreResponse(String eventId, String currentScore) {
}

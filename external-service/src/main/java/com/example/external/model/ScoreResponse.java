package com.example.external.model;

/**
 * Simple DTO returned by the External Service.
 * Using a Java 17 record for immutability & auto-generated boilerplate.
 */
public record ScoreResponse(String eventId, String currentScore) {
}

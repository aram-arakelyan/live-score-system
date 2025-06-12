package com.example.livescore.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Incoming payload for /events/status.
 */
public record EventStatusUpdate(
        @NotBlank String eventId,
        @NotNull EventStatus status   // enum, not String
) {
}

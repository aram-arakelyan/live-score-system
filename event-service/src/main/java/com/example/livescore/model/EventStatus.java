package com.example.livescore.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Canonical values accepted in POST /events/status.
 */
public enum EventStatus {
    LIVE,
    NOT_LIVE;

    /**
     * Case-insensitive factory so JSON can provide "live" or "LIVE".
     */
    @JsonCreator
    public static EventStatus from(String raw) {
        return raw == null ? null : valueOf(raw.trim().toUpperCase().replace(' ', '_'));
    }
}

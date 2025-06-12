package com.example.livescore.controller;

import com.example.livescore.model.EventStatus;
import com.example.livescore.model.EventStatusUpdate;
import com.example.livescore.store.EventStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@Slf4j
public class EventStatusController {

    private final EventStore store;

    @PostMapping("/events/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody EventStatusUpdate req) {
        String id = req.eventId();
        EventStatus stat = req.status();

        store.setStatus(id, stat);
        return ResponseEntity.accepted().build();
    }
}

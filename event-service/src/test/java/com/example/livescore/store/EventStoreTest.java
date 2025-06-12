package com.example.livescore.store;

import com.example.livescore.model.EventStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class EventStoreTest {

    @Test
    void snapshotReturnsCopyAndClears() {
        EventStore store = new EventStore(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                "test-status.json");

        store.setStatus("A", EventStatus.LIVE);
        store.setStatus("B", EventStatus.NOT_LIVE);

        var snap = store.getAndClearSnapshot();
        assertThat(snap).containsEntry("A", true)
                .containsEntry("B", false);

        assertThat(store.getAndClearSnapshot()).isEmpty();
    }

    @Test
    void concurrentWritesSafe() throws Exception {
        EventStore store = new EventStore(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                "test-status.json");

        ExecutorService exec = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 1_000; i++) {
            int idx = i;
            exec.submit(() ->
                    store.setStatus("E" + idx, idx % 2 == 0 ? EventStatus.LIVE : EventStatus.NOT_LIVE)
            );
        }
        exec.shutdown();
        exec.awaitTermination(2, TimeUnit.SECONDS);

        assertThat(store.getAndClearSnapshot()).hasSize(1_000);
    }
}

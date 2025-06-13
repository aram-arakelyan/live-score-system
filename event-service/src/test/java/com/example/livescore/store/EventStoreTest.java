package com.example.livescore.store;

import com.example.livescore.model.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class EventStoreTest {

    @Test
    void snapshotReturnsLiveOnly() {
        EventStore store = new EventStore(new ObjectMapper(), "test-status.json");

        store.setStatus("A", EventStatus.LIVE);
        store.setStatus("B", EventStatus.NOT_LIVE);

        var snap = store.snapshotLiveOnly();
        assertThat(snap)
                .containsEntry("A", true)
                .doesNotContainKey("B");   // only live events

        // NOT_LIVE removes entry
        store.setStatus("A", EventStatus.NOT_LIVE);
        assertThat(store.snapshotLiveOnly()).isEmpty();
    }

    @Test
    void concurrentWritesSafe() throws Exception {
        EventStore store = new EventStore(new ObjectMapper(), "test-status.json");

        ExecutorService exec = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 1_000; i++) {
            int idx = i;
            exec.submit(() ->
                    store.setStatus("E" + idx,
                            idx % 2 == 0 ? EventStatus.LIVE : EventStatus.NOT_LIVE));
        }
        exec.shutdown();
        exec.awaitTermination(2, TimeUnit.SECONDS);

        // Exactly 500 live entries expected
        assertThat(store.snapshotLiveOnly()).hasSize(500);
    }
}

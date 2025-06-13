package com.example.livescore.scheduler;

import com.example.livescore.service.EventScoreFetcher;
import com.example.livescore.service.ScorePublisher;
import com.example.livescore.store.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveScoreScheduler {

    private final EventStore store;
    private final EventScoreFetcher fetcher;
    private final ScorePublisher publisher;

    @Scheduled(fixedDelayString = "${live-score.polling-ms}", initialDelay = 5_000)
    public void poll() {
        store.snapshotLiveOnly().keySet().forEach(eventId -> {
            try {
                var score = fetcher.fetch(eventId);
                publisher.publish(eventId, score);
            } catch (Exception ex) {
                log.debug("Skipped {} due to exception (logged inside service)", eventId);
            }
        });
    }
}

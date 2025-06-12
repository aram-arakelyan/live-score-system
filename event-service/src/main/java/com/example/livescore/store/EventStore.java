package com.example.livescore.store;

import com.example.livescore.model.EventStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Slf4j
public class EventStore {

    private final ObjectMapper mapper;
    private final File persistence;                 // path injected
    private final Map<String, Boolean> statusMap = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public EventStore(
            ObjectMapper mapper,
            @Value("${event-store.file:event-status.json}") String filePath
    ) {
        this.mapper = mapper;
        this.persistence = new File(filePath);
    }

    /** Called by the controller threads */
    public void setStatus(String eventId, EventStatus status) {
        lock.readLock().lock();                 // shared lock: many writers allowed
        try {
            statusMap.put(eventId, status == EventStatus.LIVE);
            log.info("Event {} status → {}", eventId, status);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Called by the scheduler thread once per poll cycle */
    public Map<String, Boolean> getAndClearSnapshot() {
        lock.writeLock().lock();                // exclusive: blocks writers temporarily
        try {
            Map<String, Boolean> copy = new HashMap<>(statusMap);
            statusMap.clear();
            return copy;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @PostConstruct
    void loadFromDisk() {
        lock.writeLock().lock();                // exclusive during startup only
        try {
            if (persistence.exists()) {
                TypeReference<Map<String, Boolean>> type = new TypeReference<>() {};
                Map<String, Boolean> saved = mapper.readValue(persistence, type);
                statusMap.putAll(saved);
                log.info("Restored {} event statuses from {}", saved.size(), persistence);
            }
        } catch (Exception ex) {
            log.warn("Could not load persisted event statuses: {}", ex.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @PreDestroy
    void saveToDisk() {
        lock.writeLock().lock();                // block writers while we snapshot
        try {
            // ensure parent directory exists
            Files.createDirectories(persistence.toPath().getParent());

            // atomic write: tmp file then move
            Path tmp = Files.createTempFile(
                    persistence.getParentFile().toPath(), "evt-", ".tmp");
            mapper.writeValue(tmp.toFile(), statusMap);
            Files.move(tmp, persistence.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            log.info("Persisted {} event statuses to {}", statusMap.size(), persistence);
        } catch (Exception ex) {
            log.error("Failed to persist event statuses: {}", ex.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
}

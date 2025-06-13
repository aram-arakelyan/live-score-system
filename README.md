# Live-Score Platform ⚽️

A lightweight two-service system that

1. **ingests** _live / not-live_ status updates for sports events,
2. **polls** an external API every _10_ seconds for each live event, and
3. **publishes** the score as JSON to a Kafka topic.

Everything is container-ready — a single `docker compose up -d` starts Kafka, the external mock, and the event service.

---

## 👓 Quick glance

| Layer | Choice                              |
|-------|-------------------------------------|
| **Language** | Java 17                             |
| **Framework** | Spring Boot 3.5, Spring Kafka       |
| **Messaging** | Apache Kafka 3 (Confluent images)   |
| **Build** | Maven 3.9                           |
| **Runtime** | Docker Desktop                      |
| **Tests** | JUnit 5, AssertJ, Spring-Kafka-Test |

---

## 🚀 Setup & run

```powershell
git clone https://github.com/aram-arakelyan/live-score-system.git
cd live-score-system

# 1 – build the two jars
mvn -pl event-service,external-service clean package

# 2 – build images and start stack
docker compose build
docker compose up -d
```

## Smoke test
```powershell
# mark GAME1 as live
curl -Method POST http://localhost:8080/events/status `
     -ContentType 'application/json' `
     -Body '{"eventId":"GAME1","status":"LIVE"}'

# watch the Kafka topic
docker compose exec kafka kafka-console-consumer `
  --bootstrap-server localhost:9092 `
  --topic live-scores --from-beginning
```
## ⚙️ Configuration
| Property                         | Default                                    | Comment / Override                                         |
| -------------------------------- | ------------------------------------------ | ---------------------------------------------------------- |
| `live-score.polling-ms`          | `10000`                                    | Interval between polls (ms)                                |
| `live-score.external-base-url`   | `http://external-service:8081/api/events/` | REST source                                                |
| `score-publisher.topic`          | `live-scores`                              | Kafka topic to write to (`SCORE_TOPIC=…`)                  |
| `event-store.persistence-path`   | `event-status.json`                        | Persisted JSON state (mount with `EVENT_STORE_FILE=/data/state.json`) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092`                               | Injected by Compose                                        |

## Running tests
mvn clean verify

##  Design Decisions

### 1. EventStore

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| **In-memory map** | `ConcurrentHashMap<String,Boolean>` | Low contention, O(1) writes |
| **Concurrency** | • `setStatus` → **write-lock** (state changes)<br>• `snapshotLiveOnly` → **read-lock** (scheduler reads) | Guarantees scheduler sees a consistent map while writers run concurrently |
| **Live/Not-live semantics** | `NOT_LIVE` **removes** the key; `LIVE` **adds/overwrites** | Memory-safe and idempotent |
| **Persistence** | JSON file with atomic temp-file move | No half-written files; portable |
| **Configurable path** | `event-store.persistence-path` | Same code works local & Docker |

---

### 2. Scheduler

* Runs every `polling-ms` (default 10 s).
* Iterates **snapshotLiveOnly()** – every event still live is fetched and
  published, so a score appears in Kafka **every poll cycle** until an explicit
  `NOT_LIVE` is posted.
* Ready for multi-instance deployment (add ShedLock / Redis lock if scaling).
---

### 3. Score Publishing

| Aspect | Decision |
|--------|----------|
| **Asynchronous send** | `kafka.send(topic, json).whenComplete(...)` — non-blocking. |
| **Success/error logging** | Callback logs offset or exception (later moved to `ProducerListener` if needed). |
| **Configurable topic** | `score-publisher.topic` property; default `live-scores`. |

---

### 4. External REST Fetcher

| Aspect | Decision |
|--------|----------|
| **`RestTemplate`** | Simple blocking client; singleton bean with timeouts. |
| **URL assembly** | `UriComponentsBuilder` to avoid double-slash / encoding issues. |
| **Base URL** | Config property `live-score.external-base-url` (env override). |

---

### 5. Containers & Build

| Aspect | Decision |
|--------|----------|
| **Build strategy** | Option B: pre-build Spring Boot fat-jars (`mvn package`) then `COPY` into slim images (`eclipse-temurin:17-jre`). |
| **Compose services** | `zookeeper`, `kafka`, `external-service`, `event-service`. |
| **Persistence volume** | Host or named volume *optional*; enabled by `EVENT_STORE_FILE=/var/data


## 🤖 AI assistance
| Phase                             | Tool (ChatGPT) | Validation / refinement performed by author                    |
| --------------------------------- | -------------- | -------------------------------------------------------------- |
| Docker & Maven boilerplate        | ✅              | Adjusted paths, added Spring-Boot repackage goal               |
| Unit / integration test skeletons | ✅              | Fixed generics, injected mocks, embedded Kafka                 |
| README drafting & formatting      | ✅              | Reviewed, reordered sections, ensured Markdown renders cleanly |

All critical code — locking strategy, atomic persistence, and final refactor — was manually reviewed and tested by me.
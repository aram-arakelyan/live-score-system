package com.example.livescore.service;

import com.example.livescore.model.ScoreResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class EventScoreFetcher {

    private final RestTemplate  restTemplate;
    private final String        externalBase;   // immutable

    public EventScoreFetcher(
            RestTemplate restTemplate,
            @Value("${live-score.external-base-url}") String externalBase
    ) {
        this.restTemplate = restTemplate;
        // normalise to guarantee trailing slash
        this.externalBase = externalBase.endsWith("/") ? externalBase : externalBase + "/";
    }

    public ScoreResponse fetch(String eventId) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(externalBase)
                    .pathSegment(eventId, "score")
                    .build()
                    .toUriString();

            ResponseEntity<ScoreResponse> resp =
                    restTemplate.getForEntity(url, ScoreResponse.class);

            ScoreResponse body = resp.getBody();
            if (body == null) throw new IllegalStateException("Null body");
            return body;
        } catch (RestClientException | IllegalStateException ex) {
            log.error("External API failed for {}: {}", eventId, ex.getMessage());
            throw ex;  // let caller decide to skip / retry
        }
    }
}

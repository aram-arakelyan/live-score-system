package com.example.livescore.controller;

import com.example.livescore.model.EventStatus;
import com.example.livescore.model.EventStatusUpdate;
import com.example.livescore.store.EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventStatusController.class)
class EventStatusControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @SpyBean
    EventStore store;   // spy to verify interaction

    @Test
    void acceptsValidPayload() throws Exception {
        var body = new EventStatusUpdate("GAME1", EventStatus.LIVE);

        mvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> idCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventStatus> stCap = ArgumentCaptor.forClass(EventStatus.class);

        verify(store).setStatus(idCap.capture(), stCap.capture());
        assertThat(idCap.getValue()).isEqualTo("GAME1");
        assertThat(stCap.getValue()).isEqualTo(EventStatus.LIVE);
    }

    @Test
    void rejectsMissingEventId() throws Exception {
        String badJson = """
                    {"eventId":"", "status":"LIVE"}
                """;

        mvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }
}

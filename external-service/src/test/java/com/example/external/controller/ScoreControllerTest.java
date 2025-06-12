package com.example.external.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoreController.class)
class ScoreControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void returnsScore() throws Exception {
        mvc.perform(get("/api/events/E99/score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("E99"))
                .andExpect(jsonPath("$.currentScore").exists());
    }
}

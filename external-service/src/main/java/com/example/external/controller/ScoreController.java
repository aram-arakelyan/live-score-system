package com.example.external.controller;

import com.example.external.model.ScoreResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api/events")
public class ScoreController {

    private final Random rnd = new Random();

    @GetMapping("/{id}/score")
    public ScoreResponse getScore(@PathVariable String id) {
        // Return a pseudo-random score to simulate change
        int a = rnd.nextInt(5);
        int b = rnd.nextInt(5);
        return new ScoreResponse(id, a + ":" + b);
    }
}

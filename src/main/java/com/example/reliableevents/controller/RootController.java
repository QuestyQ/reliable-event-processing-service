package com.example.reliableevents.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "reliable-event-processing-service",
                "status", "up",
                "healthEndpoint", "/actuator/health",
                "eventsEndpoint", "POST /events"
        );
    }
}

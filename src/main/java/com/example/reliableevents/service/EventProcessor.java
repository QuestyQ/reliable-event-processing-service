package com.example.reliableevents.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface EventProcessor {
    void process(JsonNode payload);
}

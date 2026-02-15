package com.example.reliableevents.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class SimulatedEventProcessor implements EventProcessor {

    @Override
    public void process(JsonNode payload) {
        JsonNode failFlag = payload.get("forceFail");
        if (failFlag != null && failFlag.asBoolean(false)) {
            throw new ProcessingException("Simulated processing failure from payload.forceFail=true");
        }
    }
}

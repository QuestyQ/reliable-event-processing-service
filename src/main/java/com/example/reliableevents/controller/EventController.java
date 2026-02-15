package com.example.reliableevents.controller;

import com.example.reliableevents.dto.EventRequest;
import com.example.reliableevents.dto.EventResponse;
import com.example.reliableevents.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@Validated
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventResponse ingest(@RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                                @Valid @RequestBody EventRequest request) {
        return eventService.ingestEvent(idempotencyKey, request);
    }
}

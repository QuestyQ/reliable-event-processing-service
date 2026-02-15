package com.example.reliableevents.dto;

import com.example.reliableevents.domain.EventStatus;

import java.util.UUID;

public record EventResponse(
        UUID eventId,
        String idempotencyKey,
        EventStatus status,
        boolean duplicate,
        int retryCount
) {
}

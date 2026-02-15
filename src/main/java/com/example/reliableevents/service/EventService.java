package com.example.reliableevents.service;

import com.example.reliableevents.config.RetryProperties;
import com.example.reliableevents.domain.DlqEventEntity;
import com.example.reliableevents.domain.EventEntity;
import com.example.reliableevents.domain.EventStatus;
import com.example.reliableevents.dto.EventRequest;
import com.example.reliableevents.dto.EventResponse;
import com.example.reliableevents.repository.DlqEventRepository;
import com.example.reliableevents.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final DlqEventRepository dlqEventRepository;
    private final EventProcessor eventProcessor;
    private final ObjectMapper objectMapper;
    private final RetryProperties retryProperties;

    public EventService(EventRepository eventRepository,
                        DlqEventRepository dlqEventRepository,
                        EventProcessor eventProcessor,
                        ObjectMapper objectMapper,
                        RetryProperties retryProperties) {
        this.eventRepository = eventRepository;
        this.dlqEventRepository = dlqEventRepository;
        this.eventProcessor = eventProcessor;
        this.objectMapper = objectMapper;
        this.retryProperties = retryProperties;
    }

    @Transactional
    public EventResponse ingestEvent(String idempotencyKey, EventRequest request) {
        Optional<EventEntity> existing = eventRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            EventEntity event = existing.get();
            log.info("Duplicate request detected idempotencyKey={} eventId={}", idempotencyKey, event.getId());
            return toResponse(event, true);
        }

        EventEntity event = new EventEntity();
        event.setId(UUID.randomUUID());
        event.setIdempotencyKey(idempotencyKey);
        event.setCorrelationId(MDC.get("correlationId"));
        event.setStatus(EventStatus.RECEIVED);
        event.setRetryCount(0);
        event.setPayload(toJson(request));

        try {
            event = eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException exception) {
            EventEntity duplicate = eventRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
            return toResponse(duplicate, true);
        }

        processWithRetries(event, request);
        return toResponse(event, false);
    }

    private void processWithRetries(EventEntity event, EventRequest request) {
        int maxAttempts = retryProperties.getMaxAttempts();
        long backoffMs = retryProperties.getInitialBackoffMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                event.setStatus(EventStatus.PROCESSING);
                eventRepository.save(event);

                eventProcessor.process(request.getPayload());

                event.setStatus(EventStatus.PROCESSED);
                event.setRetryCount(attempt - 1);
                event.setLastError(null);
                eventRepository.save(event);
                log.info("Event processed successfully eventId={} attempt={}", event.getId(), attempt);
                return;
            } catch (Exception ex) {
                event.setRetryCount(attempt);
                event.setLastError(ex.getMessage());
                eventRepository.save(event);
                log.warn("Event processing failed eventId={} attempt={} error={}", event.getId(), attempt, ex.getMessage());

                if (attempt == maxAttempts) {
                    routeToDlq(event, ex.getMessage());
                    return;
                }
                sleep(backoffMs);
                backoffMs = Math.round(backoffMs * retryProperties.getMultiplier());
            }
        }
    }

    private void routeToDlq(EventEntity event, String reason) {
        event.setStatus(EventStatus.DLQ);
        eventRepository.save(event);

        DlqEventEntity dlq = new DlqEventEntity();
        dlq.setId(UUID.randomUUID());
        dlq.setEventId(event.getId());
        dlq.setReason(reason);
        dlq.setPayload(event.getPayload());
        dlqEventRepository.save(dlq);
        log.error("Event moved to DLQ eventId={} reason={}", event.getId(), reason);
    }

    private EventResponse toResponse(EventEntity event, boolean duplicate) {
        return new EventResponse(
                event.getId(),
                event.getIdempotencyKey(),
                event.getStatus(),
                duplicate,
                event.getRetryCount()
        );
    }

    private String toJson(EventRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getPayload());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize payload", e);
        }
    }

    private void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry sleep interrupted", interruptedException);
        }
    }
}

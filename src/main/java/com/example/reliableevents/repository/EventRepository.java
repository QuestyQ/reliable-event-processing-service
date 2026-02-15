package com.example.reliableevents.repository;

import com.example.reliableevents.domain.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {
    Optional<EventEntity> findByIdempotencyKey(String idempotencyKey);
}

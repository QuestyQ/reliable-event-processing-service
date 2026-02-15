package com.example.reliableevents.repository;

import com.example.reliableevents.domain.DlqEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DlqEventRepository extends JpaRepository<DlqEventEntity, UUID> {
}

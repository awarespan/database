package com.sip.backend.repository;

import com.sip.backend.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findByIncidentIdOrderByTimestampDesc(String incidentId);
    List<AuditEvent> findByEventTypeOrderByTimestampDesc(String eventType);
    List<AuditEvent> findAllByOrderByTimestampDesc();
}
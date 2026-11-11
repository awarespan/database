package com.sip.backend.repository;

import com.sip.backend.entity.ResponseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResponseEventRepository extends JpaRepository<ResponseEvent, String> {
    List<ResponseEvent> findByIncidentIdOrderByTimestampDesc(String incidentId);
}
package com.sip.backend.repository;

import com.sip.backend.entity.DetectionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DetectionResultRepository extends JpaRepository<DetectionResult, String> {
    Optional<DetectionResult> findByIncidentId(String incidentId);
    boolean existsByIncidentId(String incidentId);
}
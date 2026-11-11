package com.sip.backend.repository;

import com.sip.backend.entity.HumanAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HumanAnnotationRepository extends JpaRepository<HumanAnnotation, String> {
    List<HumanAnnotation> findByIncidentIdOrderByTimestampDesc(String incidentId);
    boolean existsByIncidentId(String incidentId);
}
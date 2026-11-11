package com.sip.backend.repository;

import com.sip.backend.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, String> {
    List<Evidence> findByIncidentId(String incidentId);
    List<Evidence> findByIncidentIdAndType(String incidentId, Evidence.EvidenceType type);
}
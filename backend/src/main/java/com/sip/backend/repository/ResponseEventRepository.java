package com.sip.backend.repository;

import com.sip.backend.entity.ResponseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ResponseEventRepository extends JpaRepository<ResponseEvent, String> {
    List<ResponseEvent> findByIncidentIdOrderByTimestampDesc(String incidentId);

    @Query("SELECT responseEvent FROM ResponseEvent responseEvent WHERE responseEvent.incident.id IN :incidentIds ORDER BY responseEvent.timestamp DESC")
    List<ResponseEvent> findByIncidentIdsOrderByTimestampDesc(@Param("incidentIds") List<String> incidentIds);
}
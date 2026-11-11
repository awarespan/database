package com.sip.backend.repository;

import com.sip.backend.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, String>, JpaSpecificationExecutor<Incident> {
    Page<Incident> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Incident> findByStateOrderByCreatedAtDesc(Incident.IncidentState state, Pageable pageable);

    Page<Incident> findByNodeIdOrderByCreatedAtDesc(String nodeId, Pageable pageable);

    Page<Incident> findByThreatTypeOrderByCreatedAtDesc(Incident.ThreatType threatType, Pageable pageable);

    default Page<Incident> search(
        Incident.IncidentState state,
        String nodeId,
        Incident.ThreatType threatType,
        OffsetDateTime from,
        OffsetDateTime to,
        Pageable pageable
    ) {
        Specification<Incident> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (nodeId != null && !nodeId.isBlank()) {
                predicates.add(cb.equal(root.get("node").get("id"), nodeId));
            }
            if (threatType != null) {
                predicates.add(cb.equal(root.get("threatType"), threatType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    /** Limits dashboard results in SQL instead of loading the full incidents table. */
    List<Incident> findTop10ByOrderByCreatedAtDesc();

    long countByState(Incident.IncidentState state);

    @Query("SELECT i.state, COUNT(i) FROM Incident i GROUP BY i.state")
    List<Object[]> countByState();

    Optional<Incident> findByIdempotencyKey(String idempotencyKey);
}

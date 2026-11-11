package com.sip.backend.repository;

import com.sip.backend.entity.NodeHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NodeHeartbeatRepository extends JpaRepository<NodeHeartbeat, String> {

    Optional<NodeHeartbeat> findTopByNodeIdOrderByTimestampDesc(String nodeId);

    @Query("SELECT h FROM NodeHeartbeat h WHERE h.nodeId = :nodeId ORDER BY h.timestamp DESC LIMIT :limit")
    List<NodeHeartbeat> findRecentByNodeId(@Param("nodeId") String nodeId, @Param("limit") int limit);

    @Query("SELECT h FROM NodeHeartbeat h WHERE h.timestamp > :since ORDER BY h.timestamp DESC")
    List<NodeHeartbeat> findAllSince(@Param("since") Instant since);

    void deleteByTimestampBefore(Instant cutoff);

    long countByNodeId(String nodeId);
}
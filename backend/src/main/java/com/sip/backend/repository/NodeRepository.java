package com.sip.backend.repository;

import com.sip.backend.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<Node, String> {
    List<Node> findByStatus(Node.NodeStatus status);
    long countByStatus(Node.NodeStatus status);
    Optional<Node> findByInstallationId(String installationId);

    @Modifying
    @Query("UPDATE Node n SET n.status = :status, n.lastHeartbeat = :heartbeat WHERE n.id = :id")
    int updateStatusAndHeartbeat(String id, Node.NodeStatus status, OffsetDateTime heartbeat);
}
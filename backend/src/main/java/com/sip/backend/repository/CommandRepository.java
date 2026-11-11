package com.sip.backend.repository;

import com.sip.backend.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandRepository extends JpaRepository<Command, String> {

    List<Command> findByNodeIdAndStatusOrderByPriorityDescCreatedAtAsc(String nodeId, Command.Status status);

    List<Command> findByNodeIdOrderByPriorityDescCreatedAtAsc(String nodeId);

    Optional<Command> findFirstByNodeIdAndStatusOrderByPriorityDescCreatedAtAsc(String nodeId, Command.Status status);

    @Query("SELECT c FROM Command c WHERE c.status = :status AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<Command> findActiveByStatus(@Param("status") Command.Status status, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Command c SET c.status = 'EXPIRED' WHERE c.status = 'PENDING' AND c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    int expireOldCommands(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Command c SET c.status = :newStatus, c.dispatchedAt = :dispatchedAt WHERE c.id = :id AND c.status = 'PENDING'")
    int markDispatched(@Param("id") String id, @Param("newStatus") Command.Status newStatus, @Param("dispatchedAt") Instant dispatchedAt);

    @Modifying
    @Query("UPDATE Command c SET c.status = 'ACKNOWLEDGED', c.acknowledgedAt = :ackAt WHERE c.id = :id AND c.status = 'DISPATCHED'")
    int markAcknowledged(@Param("id") String id, @Param("ackAt") Instant ackAt);

    @Modifying
    @Query("UPDATE Command c SET c.status = 'FAILED', c.failureReason = :reason WHERE c.id = :id")
    int markFailed(@Param("id") String id, @Param("reason") String reason);

    long countByNodeIdAndStatus(String nodeId, Command.Status status);
}
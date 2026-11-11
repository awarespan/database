package com.sip.backend.service;

import com.sip.backend.entity.Command;
import com.sip.backend.entity.Node;
import com.sip.backend.repository.CommandRepository;
import com.sip.backend.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Service
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private final CommandRepository commandRepository;
    private final NodeRepository nodeRepository;
    private final MqttPublisher mqttPublisher;

    public CommandService(CommandRepository commandRepository, NodeRepository nodeRepository,
                          @Autowired(required = false) MqttPublisher mqttPublisher) {
        this.commandRepository = commandRepository;
        this.nodeRepository = nodeRepository;
        this.mqttPublisher = mqttPublisher;
    }

    @Transactional
    public Command dispatchCommand(String nodeId, Command.CommandType type, Map<String, Object> payload, int priority, int ttlSeconds) {
        if (!nodeRepository.existsById(nodeId)) {
            throw new IllegalArgumentException("Unknown node: " + nodeId);
        }

        Command cmd = new Command(nodeId, type, payload, priority);
        if (ttlSeconds > 0) {
            cmd.setExpiresAt(Instant.now().plus(ttlSeconds, ChronoUnit.SECONDS));
        }
        cmd = commandRepository.save(cmd);

        // Publish to MQTT for the specific node (no-op if MQTT disabled)
        if (mqttPublisher != null) {
            mqttPublisher.publishCommand(nodeId, cmd);
        }

        log.info("Dispatched command {} type={} to node {}", cmd.getId(), type, nodeId);
        return cmd;
    }

    public List<Command> getPendingForNode(String nodeId) {
        return commandRepository.findByNodeIdAndStatusOrderByPriorityDescCreatedAtAsc(nodeId, Command.Status.PENDING);
    }

    @Transactional
    public void acknowledgeCommand(String commandId) {
        commandRepository.markAcknowledged(commandId, Instant.now());
    }

    @Transactional
    public void failCommand(String commandId, String reason) {
        commandRepository.markFailed(commandId, reason);
    }

    @Transactional
    public void expireOldCommands() {
        int expired = commandRepository.expireOldCommands(Instant.now());
        if (expired > 0) log.info("Expired {} commands", expired);
    }

    public Optional<Command> findById(String id) {
        return commandRepository.findById(id);
    }

    public List<Command> findByNodeId(String nodeId) {
        return commandRepository.findByNodeIdOrderByPriorityDescCreatedAtAsc(nodeId);
    }
}
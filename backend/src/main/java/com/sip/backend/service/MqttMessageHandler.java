package com.sip.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sip.backend.dto.NodeHeartbeatPayload;
import com.sip.backend.dto.PiIncidentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = false)
public class MqttMessageHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final PiIncidentIngestionService incidentService;
    private final PiAuthenticationService authService;
    private final MqttPublisher mqttPublisher;
    private final NodeService nodeService;

    public MqttMessageHandler(ObjectMapper objectMapper, PiIncidentIngestionService incidentService,
                              PiAuthenticationService authService, MqttPublisher mqttPublisher,
                              NodeService nodeService) {
        this.objectMapper = objectMapper;
        this.incidentService = incidentService;
        this.authService = authService;
        this.mqttPublisher = mqttPublisher;
        this.nodeService = nodeService;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = (String) message.getHeaders().get("mqtt_topic");
        String payloadStr = new String((byte[]) message.getPayload());

        log.debug("MQTT message on topic {}: {}", topic, payloadStr);

        if (topic == null) return;

        try {
            if (topic.matches("sip/[^/]+/incidents")) {
                handleIncidentMessage(topic, payloadStr);
            } else if (topic.matches("sip/[^/]+/heartbeat")) {
                handleHeartbeatMessage(topic, payloadStr);
            } else if (topic.matches("sip/[^/]+/command/ack")) {
                handleCommandAck(topic, payloadStr);
            } else if (topic.matches("sip/[^/]+/command/nack")) {
                handleCommandNack(topic, payloadStr);
            } else {
                log.debug("Unhandled MQTT topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error processing MQTT message on topic {}: {}", topic, e.getMessage());
        }
    }

    private void handleIncidentMessage(String topic, String payloadStr) throws Exception {
        String nodeId = topic.split("/")[1];
        PiIncidentPayload payload = objectMapper.readValue(payloadStr, PiIncidentPayload.class);
        payload.setNodeId(nodeId);

        try {
            incidentService.ingestIncident(payload);
            mqttPublisher.publishIncidentAcknowledgement(nodeId, payload.getIncidentId(), true);
        } catch (Exception e) {
            log.error("Failed to ingest incident from node {}: {}", nodeId, e.getMessage());
            mqttPublisher.publishIncidentAcknowledgement(nodeId, payload.getIncidentId(), false);
        }
    }

    private void handleHeartbeatMessage(String topic, String payloadStr) throws Exception {
        String nodeId = topic.split("/")[1];
        NodeHeartbeatPayload payload = objectMapper.readValue(payloadStr, NodeHeartbeatPayload.class);
        payload.setNodeId(nodeId);
        nodeService.recordHeartbeat(nodeId, payload);
    }

    private void handleCommandAck(String topic, String payloadStr) throws Exception {
        String nodeId = topic.split("/")[1];
        var ack = objectMapper.readTree(payloadStr);
        String commandId = ack.get("commandId").asText();
        log.info("Command {} acknowledged by node {}", commandId, nodeId);
        // TODO: Mark command as acknowledged
    }

    private void handleCommandNack(String topic, String payloadStr) throws Exception {
        String nodeId = topic.split("/")[1];
        var nack = objectMapper.readTree(payloadStr);
        String commandId = nack.get("commandId").asText();
        String reason = nack.has("reason") ? nack.get("reason").asText() : "unknown";
        log.warn("Command {} rejected by node {}: {}", commandId, nodeId, reason);
        // TODO: Mark command as failed
    }
}
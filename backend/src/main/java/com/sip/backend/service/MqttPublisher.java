package com.sip.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sip.backend.config.MqttConfigProperties;
import com.sip.backend.entity.Command;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = false)
public class MqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisher.class);
    private static final int QOS = 1;
    private static final int OPERATION_TIMEOUT_SECS = 30;

    private final MqttConfigProperties props;
    private final MqttConnectOptions connectOptions;
    private final ObjectMapper objectMapper;
    private MqttClient mqttClient;

    public MqttPublisher(MqttConfigProperties props, MqttConnectOptions mqttConnectOptions, ObjectMapper objectMapper) {
        this.props = props;
        this.connectOptions = mqttConnectOptions;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            String brokerUrl = props.getBrokerUrl();
            String clientId = "sip-backend-" + UUID.randomUUID().toString().substring(0, 8);
            mqttClient = new MqttClient(brokerUrl, clientId, null);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT connection lost: {}", cause != null ? cause.getMessage() : "unknown");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Outbound-only publisher — no inbound messages expected
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Delivery confirmation
                }
            });
            mqttClient.connect(connectOptions);
            log.info("MQTT publisher connected to {} with clientId {}", brokerUrl, clientId);
        } catch (MqttException e) {
            log.error("Failed to connect MQTT publisher: {}", e.getMessage());
            throw new RuntimeException("MQTT connection failed", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                log.info("MQTT publisher disconnected");
            } catch (MqttException e) {
                log.warn("Error disconnecting MQTT publisher: {}", e.getMessage());
            }
        }
    }

    public void publishCommand(String nodeId, Command command) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("commandId", command.getId());
            payload.put("type", command.getCommandType().name());
            payload.put("priority", command.getPriority());
            payload.put("createdAt", command.getCreatedAt().toString());
            payload.put("payload", command.getPayload());
            if (command.getExpiresAt() != null) {
                payload.put("expiresAt", command.getExpiresAt().toString());
            }

            String json = objectMapper.writeValueAsString(payload);
            String topic = "sip/" + nodeId + "/commands";

            publish(topic, json, QOS);
            log.debug("Published command {} to topic {}", command.getId(), topic);
        } catch (Exception e) {
            log.error("Failed to publish command {} to node {}: {}", command.getId(), nodeId, e.getMessage());
        }
    }

    public void publishIncidentAcknowledgement(String nodeId, String incidentId, boolean success) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("incidentId", incidentId);
            payload.put("success", success);

            String json = objectMapper.writeValueAsString(payload);
            String topic = "sip/" + nodeId + "/ack";

            publish(topic, json, 0);
        } catch (Exception e) {
            log.error("Failed to publish ack for incident {} to node {}: {}", incidentId, nodeId, e.getMessage());
        }
    }

    private void publish(String topic, String payload, int qos) throws MqttException {
        ensureConnected();
        mqttClient.publish(topic, payload.getBytes(), qos, false);
    }

    private void ensureConnected() throws MqttException {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.info("MQTT client not connected, reconnecting...");
            if (mqttClient == null) {
                String brokerUrl = props.getBrokerUrl();
                String clientId = "sip-backend-" + UUID.randomUUID().toString().substring(0, 8);
                mqttClient = new MqttClient(brokerUrl, clientId, null);
            }
            mqttClient.connect(connectOptions);
        }
    }
}
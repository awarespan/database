package com.sip.backend.service;

import com.sip.backend.config.MqttConfigProperties;
import com.sip.backend.entity.Node;
import com.sip.backend.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
public class PiAuthenticationService {

    private final NodeRepository nodeRepository;
    private final String credentialSalt;

    public PiAuthenticationService(
            NodeRepository nodeRepository,
            @Value("${app.node.credential-salt:}") String credentialSalt) {
        this.nodeRepository = nodeRepository;
        this.credentialSalt = credentialSalt != null ? credentialSalt : "";
    }

    /**
     * Derives the per-node MQTT credential hash.
     * Node sends a token derived from: nodeId + deviceSecret + salt
     * We verify by computing the same hash and comparing.
     */
    public Optional<String> deriveNodeCredential(String nodeId, String deviceSecret) {
        if (nodeId == null || deviceSecret == null) return Optional.empty();
        try {
            String input = nodeId + ":" + deviceSecret + ":" + credentialSalt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Optional.of(Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException e) {
            return Optional.empty();
        }
    }

    /**
     * Validates a node credential token.
     * The node computes: SHA256(nodeId + deviceSecret + salt) and sends it.
     * We look up the node by nodeId, get its secret, recompute, and compare.
     */
    public boolean validateNodeToken(String nodeId, String presentedToken) {
        Optional<Node> nodeOpt = nodeRepository.findById(nodeId);
        if (nodeOpt.isEmpty()) return false;
        Node node = nodeOpt.get();
        String deviceSecret = node.deviceSecret != null ? node.deviceSecret : nodeId;
        Optional<String> expectedToken = deriveNodeCredential(nodeId, deviceSecret);
        return expectedToken.map(t ->
            MessageDigest.isEqual(t.getBytes(StandardCharsets.UTF_8), presentedToken.getBytes(StandardCharsets.UTF_8))
        ).orElse(false);
    }

    /**
     * Validates a raw device secret presented in an MQTT message payload.
     * The secret is the device-specific secret configured on the Pi.
     */
    public boolean validateDeviceSecret(String nodeId, String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) return false;
        Optional<Node> nodeOpt = nodeRepository.findById(nodeId);
        if (nodeOpt.isEmpty()) return false;
        Node node = nodeOpt.get();
        String storedSecret = node.deviceSecret != null ? node.deviceSecret : nodeId;
        return MessageDigest.isEqual(
                storedSecret.getBytes(StandardCharsets.UTF_8),
                rawSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
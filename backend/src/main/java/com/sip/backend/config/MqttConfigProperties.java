package com.sip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.mqtt")
public class MqttConfigProperties {

    private String brokerUrl = "tcp://localhost:1883";
    private String username;
    private String password;
    private String clientId = "sip-backend";
    private int commandTimeout = 30;
    private int keepAliveInterval = 60;
    private boolean useTls = false;

    public String getBrokerUrl() { return brokerUrl; }
    public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public int getCommandTimeout() { return commandTimeout; }
    public void setCommandTimeout(int commandTimeout) { this.commandTimeout = commandTimeout; }
    public int getKeepAliveInterval() { return keepAliveInterval; }
    public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }
    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }
}
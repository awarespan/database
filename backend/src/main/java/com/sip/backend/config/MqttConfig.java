package com.sip.backend.config;

import com.sip.backend.config.MqttConfigProperties;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = false)
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttConfigProperties props) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{props.getBrokerUrl()});
        options.setKeepAliveInterval(props.getKeepAliveInterval());
        options.setConnectionTimeout(props.getCommandTimeout());
        options.setAutomaticReconnect(true);

        if (props.getUsername() != null && !props.getUsername().isBlank()) {
            options.setUserName(props.getUsername());
        }
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            options.setPassword(props.getPassword().toCharArray());
        }

        if (props.isUseTls()) {
            java.util.Properties sslProps = new java.util.Properties();
            sslProps.setProperty("ssl.protocol", "TLSv1.2");
            sslProps.setProperty("ssl.handshake.timeout", "30");
            options.setSSLProperties(sslProps);
        }

        log.info("MQTT configured for broker: {} (TLS={})", props.getBrokerUrl(), props.isUseTls());
        return options;
    }
}
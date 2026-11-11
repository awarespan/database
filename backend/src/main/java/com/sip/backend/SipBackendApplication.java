package com.sip.backend;

import com.sip.backend.config.DataSourceUriParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Entry point. DATABASE_URL is parsed and its components are set as system
 * properties BEFORE SpringApplication.run() so all property placeholder
 * resolution picks them up, including datasource initialization.
 */
@SpringBootApplication
public class SipBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(SipBackendApplication.class);

    public static void main(String[] args) {
        // Set system properties from DATABASE_URL BEFORE Spring loads anything
        DataSourceUriParser.populateSystemProperties();

        SpringApplication app = new SpringApplication(SipBackendApplication.class);
        app.run(args);
    }
}
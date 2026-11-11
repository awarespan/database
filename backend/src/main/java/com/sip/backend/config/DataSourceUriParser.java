package com.sip.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Parses a Neon DATABASE_URL (e.g. postgresql://user:pass@host/db?sslmode=require)
 * and sets DB_HOST, DB_NAME, DB_USER, DB_PASSWORD, DB_QUERY as SYSTEM PROPERTIES.
 *
 * Must be called from main() BEFORE SpringApplication.run() so Spring's property
 * placeholder resolution sees these values for spring.datasource.* properties.
 */
public class DataSourceUriParser {

    private static final Logger log = LoggerFactory.getLogger(DataSourceUriParser.class);

    private DataSourceUriParser() {}

    /**
     * Parses DATABASE_URL and sets system properties for the datasource.
     * Safe to call multiple times (checks if already set).
     */
    public static void populateSystemProperties() {
        // Always read env var directly — available from the JVM's first breath
        String uri = System.getenv("DATABASE_URL");

        if (uri == null || uri.isBlank()) {
            log.warn("DATABASE_URL env var is not set — datasource will use application.yml defaults");
            return;
        }

        // Strip jdbc:postgresql:// prefix if present (Render may set DATABASE_URL
        // in that form rather than the raw postgresql:// scheme URI expects)
        if (uri.startsWith("jdbc:")) {
            uri = uri.substring(5);
        }

        // Avoid double-setting if already done
        if (System.getProperty("DB_HOST") != null) {
            log.debug("DB_HOST already set, skipping DATABASE_URL parse");
            return;
        }

        try {
            URI parsed = URI.create(uri);

            String userInfo = parsed.getRawUserInfo();
            String dbUser = "";
            String dbPassword = "";
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    dbUser = URLDecoder.decode(userInfo.substring(0, colon), StandardCharsets.UTF_8);
                    dbPassword = URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);
                } else {
                    dbUser = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                }
            }

            String hostPort = parsed.getHost();
            if (hostPort == null) {
                throw new IllegalArgumentException("No host in DATABASE_URL: " + uri);
            }
            if (parsed.getPort() != -1) {
                hostPort += ":" + parsed.getPort();
            }

            String dbName = parsed.getPath();
            if (dbName != null && dbName.startsWith("/")) {
                dbName = dbName.substring(1);
            }
            if (dbName == null || dbName.isBlank()) {
                dbName = "neondb";
            }

            String query = parsed.getRawQuery();
            if (query == null) query = "";
            if (!query.contains("sslmode")) {
                query = (query.isEmpty() ? "" : query + "&") + "sslmode=require";
            }

            // Set as system properties — highest priority, visible to Spring everywhere
            System.setProperty("DB_HOST", hostPort);
            System.setProperty("DB_NAME", dbName);
            System.setProperty("DB_USER", dbUser);
            System.setProperty("DB_PASSWORD", dbPassword);
            System.setProperty("DB_QUERY", query);

            log.info("DATABASE_URL parsed → host={}, db={}, user={}, sslmode={}",
                    hostPort, dbName,
                    dbUser.isEmpty() ? "(none)" : dbUser,
                    query.contains("sslmode=require") ? "require" : "not-set");

        } catch (Exception e) {
            log.error("Failed to parse DATABASE_URL '{}': {}", uri, e.getMessage());
        }
    }
}
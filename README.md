# SIP Backend — Database & Backend Service

Spring Boot 3.4.1 / Java 17 backend for the Street Safety Intelligence Platform (SIP).

## What this does

| Layer | Technology | Purpose |
|-------|-----------|---------|
| REST API | Spring Boot | Android app, Pi REST fallback |
| WebSocket | Spring WebSocket | Real-time incident dashboard |
| MQTT | Spring Integration + Eclipse Paho | Pi ↔ backend (solves dynamic IP) |
| Database | Neon PostgreSQL | Persistent storage |
| Migrations | Flyway | Schema versioning |
| Auth | JWT + rotating refresh tokens | User sessions |
| Device Auth | X-API-Key | Pi device authentication |

## Prerequisites

- **Java 17+** (`java -version`)
- **Gradle 8+** (`gradle -v` or use the wrapper)
- **Neon PostgreSQL** — JDBC connection string required (NOT API key)
  - Create at [neon.tech](https://neon.tech) → Connection Details → JDBC

## Quick Start

```bash
cd database/backend

# Copy and fill in environment
cp .env.example .env
# Edit .env with your DATABASE_URL, JWT_SECRET, DEVICE_API_KEY

# Build
./gradlew bootJar

# Run
java -jar build/libs/sip-backend-0.1.0.jar
```

The backend runs on port **8080** by default.

## Environment Variables

### Required

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC PostgreSQL URL — e.g. `jdbc:postgresql://host/db?user=user&password=pass&sslmode=require` |
| `JWT_SECRET` | JWT signing key — min 32 chars. Generate: `openssl rand -hex 32` |
| `DEVICE_API_KEY` | API key for Pi devices — generate: `openssl rand -hex 32` |

### Optional

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP port |
| `JWT_ACCESS_EXPIRY` | `900` | Access token TTL (seconds) |
| `JWT_REFRESH_EXPIRY` | `604800` | Refresh token TTL (seconds) |
| `MQTT_ENABLED` | `true` | Enable MQTT subscriber/publisher |
| `MQTT_BROKER_URL` | `tcp://localhost:1883` | MQTT broker URL |
| `MQTT_USERNAME` | `` | MQTT username |
| `MQTT_PASSWORD` | `` | MQTT password |
| `NODE_CREDENTIAL_SALT` | `` | Salt for per-node credential derivation |
| `CORS_ALLOWED_ORIGINS` | `` | Comma-separated allowed origins |
| `SIP_BOOTSTRAP_ADMIN_USERNAME` | `admin` | Initial admin username |
| `SIP_BOOTSTRAP_ADMIN_PASSWORD` | `admin` | Initial admin password |

## Database Migrations

Migrations live in `src/main/resources/db/migration/`. They run automatically on startup.

| Migration | Description |
|-----------|-------------|
| `V1__initial_schema.sql` | Core tables: users, nodes, incidents, detections, etc. |
| `V6__enhanced_schema.sql` | Pi integration: heartbeats, command queue, datasets, training runs |

## API Reference

### Authentication
```
POST /api/v1/auth/login         → { accessToken, refreshToken }
POST /api/v1/auth/refresh       → { accessToken }
POST /api/v1/auth/logout        → 204
```

### Incidents
```
GET    /api/v1/incidents        → paginated list
GET    /api/v1/incidents/{id}   → incident detail
POST   /api/v1/incidents        → ingest from Pi (X-API-Key auth)
PUT    /api/v1/incidents/{id}/verify
PUT    /api/v1/incidents/{id}/reject
PUT    /api/v1/incidents/{id}/escalate
PUT    /api/v1/incidents/{id}/resolve
```

### Nodes
```
GET    /api/v1/nodes            → list all
GET    /api/v1/nodes/{id}       → node detail
POST   /api/v1/nodes            → register (X-API-Key)
POST   /api/v1/nodes/{id}/heartbeat → record heartbeat (X-API-Key)
PUT    /api/v1/nodes/{id}/status
```

### Commands (Pi management)
```
POST   /api/v1/commands/dispatch/{nodeId}  → send command to Pi
GET    /api/v1/commands/node/{nodeId}
GET    /api/v1/commands/pending/{nodeId}
POST   /api/v1/commands/{id}/acknowledge
```

### WebSocket
```
/ws    → real-time incident updates (JSON frames)
```

## Pi Integration

See [docs/PI_INTEGRATION.md](docs/PI_INTEGRATION.md) for full Pi setup guide.

The Pi connects OUT to the MQTT broker — **no static IP or port forwarding needed**.

## Deployment Options

| Platform | Notes |
|----------|-------|
| Railway | Set env vars, deploy JAR, add a public MQ broker |
| Render | Similar to Railway — `render.yaml` template in `docs/` |
| Fly.io | `fly launch` + `fly secrets set` |
| Docker | `docker build` + `docker run` with env vars |
| VPS | Run JAR directly or via Docker Compose |

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for detailed instructions.

## Testing

```bash
./gradlew test                    # Unit tests (H2 in-memory)
./gradlew test --tests '*IT'      # Integration tests (Testcontainers + Neon)
```

## Project Structure

```
backend/
├── build.gradle
├── settings.gradle
├── .env.example
└── src/main/
    ├── java/com/sip/backend/
    │   ├── SipBackendApplication.java
    │   ├── auth/          # JWT filter, auth controller
    │   ├── config/        # Security, MQTT, WebSocket, CORS
    │   ├── controller/    # REST controllers
    │   ├── dto/           # Request/response DTOs
    │   ├── entity/        # JPA entities
    │   ├── repository/    # Spring Data repos
    │   ├── service/       # Business logic
    │   └── websocket/     # WebSocket config & handlers
    └── resources/
        ├── application.yml
        └── db/migration/  # Flyway SQL migrations
```
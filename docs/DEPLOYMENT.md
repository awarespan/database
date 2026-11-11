# Deployment Guide

## Target Environments

### 1. Railway (Recommended for fast deploys)

```bash
# Install Railway CLI
npm install -g @railway/cli
railway login

# From database/backend/
railway init
railway add --variable DATABASE_URL "$DATABASE_URL"
railway add --variable JWT_SECRET "$JWT_SECRET"
railway add --variable DEVICE_API_KEY "$DEVICE_API_KEY"
railway add --variable MQTT_BROKER_URL "$MQTT_BROKER_URL"
railway add --variable CORS_ALLOWED_ORIGINS "https://kev-ai-device.vercel.app"
railway add --variable SIP_BOOTSTRAP_ADMIN_USERNAME "admin"
railway add --variable SIP_BOOTSTRAP_ADMIN_PASSWORD "your-secure-password"

# Deploy
railway up --detach
railway domain
```

Railway will auto-detect Spring Boot and build with Gradle.

### 2. Render

```yaml
# render.yaml (place in database/backend/)
services:
  - name: sip-backend
    buildCommand: ./gradlew bootJar
    startCommand: java -jar build/libs/sip-backend-0.1.0.jar
    envVars:
      - key: DATABASE_URL
        sync: false
      - key: JWT_SECRET
        sync: false
      - key: DEVICE_API_KEY
        sync: false
      - key: MQTT_BROKER_URL
        value: "tcp://broker.hivemq.com:1883"
      - key: CORS_ALLOWED_ORIGINS
        value: "https://kev-ai-device.vercel.app"
      - key: JAVA_OPTS
        value: "-Xmx512m"
```

### 3. Docker

```dockerfile
# database/backend/Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/sip-backend-0.1.0.jar app.jar
COPY .env.example /.env
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
cd database/backend
./gradlew bootJar
docker build -t sip-backend .
docker run -p 8080:8080 \
  --env-file .env \
  sip-backend
```

### 4. Fly.io

```bash
cd database/backend
fly launch --no-deploy
fly secrets set DATABASE_URL="$DATABASE_URL"
fly secrets set JWT_SECRET="$JWT_SECRET"
fly secrets set DEVICE_API_KEY="$DEVICE_API_KEY"
fly secrets set MQTT_BROKER_URL="tcp://broker.hivemq.com:1883"
fly secrets set CORS_ALLOWED_ORIGINS="https://kev-ai-device.vercel.app"
fly deploy
fly ips show
```

## Environment Setup Checklist

### Backend (.env)

```bash
# Generate secrets
export JWT_SECRET=$(openssl rand -hex 32)
export DEVICE_API_KEY=$(openssl rand -hex 32)
export NODE_CREDENTIAL_SALT=$(openssl rand -hex 16)

# Database (Neon)
export DATABASE_URL="jdbc:postgresql://ep-xxx.neon.tech/neondb?user=your-user&password=your-pass&sslmode=require"

# CORS (your Vercel domain)
export CORS_ALLOWED_ORIGINS="https://kev-ai-device.vercel.app"

# MQTT (public HiveMQ broker — no auth)
export MQTT_BROKER_URL="tcp://broker.hivemq.com:1883"
```

### Pi (.env on the Raspberry Pi)

```bash
export MQTT_BROKER_URL="tcp://broker.hivemq.com:1883"
export BACKEND_API_URL="https://your-backend.railway.app"   # After deploy
export BACKEND_API_KEY="<same DEVICE_API_KEY as backend>"
export PI_NODE_ID="sip-pi-001"
export PI_DEVICE_SECRET=$(openssl rand -hex 32)
```

## Domain & HTTPS

All traffic should be HTTPS:

- **Railway**: Auto-provisions HTTPS via Railway's proxy
- **Render**: Auto-provisions HTTPS via Render's load balancer
- **Fly.io**: Auto-provisions HTTPS via Let's Encrypt
- **VPS**: Set up nginx/Caddy with Let's Encrypt

## MQTT Broker Options

| Broker | Free Tier | Notes |
|--------|-----------|-------|
| HiveMQ Cloud | 100 connections | Recommended — no config needed |
| Mosquitto (self-hosted) | Free | Run on same VPS as backend |
| EMQX | 1000 connections | More features, needs setup |
| CloudMQTT | 25 connections | CuteCat sunsetting |

For HiveMQ Cloud:
1. Sign up at [hivemq.cloud](https://www.hivemq.cloud)
2. Create a cluster → Get "Cluster URL" and credentials
3. Set `MQTT_BROKER_URL=tcp://xxx.hivemq.cloud:1883`
4. Set `MQTT_USERNAME` and `MQTT_PASSWORD`

## Vercel Frontend

Update `CORS_ALLOWED_ORIGINS` to include your Vercel domain:
```
CORS_ALLOWED_ORIGINS=https://kev-ai-device.vercel.app
```

The backend REST API URL for the Android app:
```
https://your-backend.railway.app/api/v1
```

## Health Check

After deploy:
```bash
curl https://your-backend.railway.app/actuator/health
# Expected: {"status":"UP"}
```

## Common Issues

### "Connection refused" on Pi
- Pi can't reach the backend URL — verify the URL is publicly accessible
- Test from your phone: `curl https://your-backend.railway.app/actuator/health`

### MQTT not connecting
- Check broker URL is correct and port 1883 is not blocked
- HiveMQ Cloud requires username/password if auth is enabled

### CORS errors in browser
- Verify `CORS_ALLOWED_ORIGINS` includes `https://kev-ai-device.vercel.app`
- Check no trailing spaces in the env var

### Flyway migration fails
- Check `DATABASE_URL` is a valid JDBC URL (not the Neon management API key)
- Ensure the database user has schema permissions
# Pi Integration Guide

## Architecture Overview

```
                    ┌─────────────────────────────────────────────────┐
                    │              MQTT Broker                        │
                    │   (HiveMQ Cloud / Mosquitto / EMQX)             │
                    │   broker.hivemq.com:1883 (public)               │
                    └───────────────┬─────────────────────────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            │                                               │
      ┌─────▼──────┐                               ┌────────▼────────┐
      │   Pi 5     │                               │   Backend       │
      │            │                               │  (Spring Boot)  │
      │ MQTT Pub   │─────── incidents ──────────► │                 │
      │ MQTT Sub   │◄──────── commands ──────────│ MQTT Sub/Pub     │
      │            │                               │                 │
      │ REST POST  │─────── fallback ───────────► │ REST API        │
      │ (offline)  │                               │                 │
      └────────────┘                               └────────┬────────┘
                                                             │
                                                    Neon PostgreSQL
```

**Key insight**: The Pi connects OUT to the MQTT broker and the REST backend. The backend never needs to reach the Pi's IP. This solves the dynamic IP problem entirely.

## Quick Start

### 1. Copy files to Pi

```bash
# On your Pi:
mkdir -p ~/edge-ai
cd ~/edge-ai
# Copy these files from node-backend/pi/:
#   edge_server.py, event_queue.py, mqtt_client.py, backend_sync.py
#   config.json, requirements.txt, start.sh, sip-edge.service
```

### 2. Install dependencies

```bash
pip install opencv-python-headless numpy ultralytics websockets requests paho-mqtt python-dotenv
```

### 3. Configure environment

```bash
cat > ~/edge-ai/.env << 'EOF'
# MQTT — public HiveMQ broker (no auth needed to start)
MQTT_BROKER_URL=tcp://broker.hivemq.com:1883

# Backend URL (set after deploying backend)
BACKEND_API_URL=https://your-backend.railway.app
BACKEND_API_KEY=your-device-api-key

# Pi identity (must match what's registered in the backend)
PI_NODE_ID=sip-pi-001
PI_DEVICE_SECRET=change-me

# Telegram (optional)
BOT_TOKEN=your-telegram-bot-token
PRIMARY_CHAT_ID=your-chat-id
EOF
```

### 4. Register the node in the backend

```bash
curl -X POST https://your-backend.com/api/v1/nodes \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-device-api-key" \
  -d '{
    "id": "sip-pi-001",
    "name": "Living Room Pi",
    "status": "ONLINE",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "firmwareVersion": "1.0.0"
  }'
```

### 5. Install the systemd service

```bash
# As pi user:
mkdir -p ~/edge-ai/logs
chmod +x ~/edge-ai/start.sh
sudo cp ~/edge-ai/sip-edge.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable sip-edge
sudo systemctl start sip-edge
```

### 6. Verify

```bash
# Check logs
journalctl -u sip-edge -f
tail -f ~/edge-ai/logs/edge.log

# Check MQTT connection
python3 -c "
import sys, os, event_queue
event_queue.start()
import time; time.sleep(3)
print('MQTT connected:', event_queue.is_mqtt_connected())
print('Queue depth:', event_queue.get_queue_depth())
event_queue.stop()
"
```

## MQTT Topics

| Topic | Direction | QoS | Payload |
|-------|-----------|-----|---------|
| `sip/{nodeId}/incidents` | Pi → Backend | 1 | `PiIncidentPayload` JSON |
| `sip/{nodeId}/heartbeat` | Pi → Backend | 0 | Heartbeat JSON |
| `sip/{nodeId}/commands` | Backend → Pi | 2 | Command JSON |
| `sip/{nodeId}/command/ack` | Pi → Backend | 0 | `{"commandId":"..."}` |
| `sip/{nodeId}/command/nack` | Pi → Backend | 0 | `{"commandId":"...","reason":"..."}` |

## Incident Payload (Pi → Backend)

```json
{
  "incidentId": "inc-a1b2c3d4",
  "idempotencyKey": "sip-pi-001-inc-a1b2c3d4",
  "nodeId": "sip-pi-001",
  "detectionTime": "2025-10-08T12:34:56Z",
  "threatType": "dog,cat",
  "threatSeverity": "LOW",
  "threatDescription": "Animal detected near entry",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "locationReadable": "123 Main St",
  "locationAccuracy": 5.0,
  "modelVersion": "yolov8n.pt",
  "confidence": 0.87,
  "detections": [
    {"label": "dog", "confidence": 0.87},
    {"label": "cat", "confidence": 0.45}
  ],
  "tier": 1,
  "detectionDomain": "ANIMAL",
  "syncStatus": "PENDING"
}
```

## Command Payload (Backend → Pi)

```json
{
  "commandId": "cmd-x1y2z3",
  "type": "SIREN",
  "priority": 5,
  "createdAt": "2025-10-08T12:35:00Z",
  "payload": {"durationSeconds": 10, "volume": 80},
  "expiresAt": "2025-10-08T12:40:00Z"
}
```

## Offline Behavior

When the Pi loses connectivity:

1. **MQTT disconnected**: Incidents are queued in `/tmp/sip_offline_queue.db` (SQLite)
2. **Both MQTT + REST down**: Incidents go to `/tmp/sip_incident_queue.db`
3. **Reconnect**: Queue is replayed automatically every 30 seconds
4. **Max queue age**: 48 hours / 20 retry attempts — then purged

## Dynamic IP Solution: Cloudflare Tunnel

The Pi's IP changes on your home network. Instead of trying to reach it, the Pi reaches OUT to:
- MQTT broker (public internet)
- Backend REST API (public URL)

For the **edge_server WebSocket dashboard** to be accessible from outside:
```bash
# Install cloudflared on the Pi:
curl -fsSL https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 \
  -o /usr/local/bin/cloudflared && chmod +x /usr/local/bin/cloudflared

# Run a quick tunnel (generates a random URL):
cloudflared tunnel --url http://localhost:8766

# Or use a named tunnel (stable URL via Cloudflare dashboard):
cloudflared tunnel --token YOUR_TOKEN run
```

The tunnel URL (e.g., `https://random-name.trycloudflare.com`) is logged to `~/edge-ai/logs/tunnel.log`.

## Commands Available

| Command | Description | Payload |
|---------|-------------|---------|
| `SIREN` | Trigger deterrent siren | `{"durationSeconds": 10, "volume": 80}` |
| `SILENCE` | Stop siren | `{}` |
| `MODEL_UPDATE` | Download new YOLO model | `{"modelUrl": "https://...", "checksum": "sha256:..."}` |
| `CONFIG_SYNC` | Pull updated threat policy | `{"configVersion": "2"}` |
| `REBOOT` | Restart the Pi | `{"delaySeconds": 5}` |
| `SET_AUTOPILOT` | Enable/disable autopilot | `{"enabled": true, "policy": {...}}` |
| `EMERGENCY_STOP` | Stop all deterrence | `{}` |

## Securing the Pi

```bash
# Set a strong device secret (used to derive MQTT credential hash)
PI_DEVICE_SECRET=$(openssl rand -hex 32)
echo "PI_DEVICE_SECRET=$PI_DEVICE_SECRET" >> ~/edge-ai/.env

# Never commit .env to git
echo ".env" >> ~/.gitignore_global
```
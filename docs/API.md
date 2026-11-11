# SIP Backend API

## Base URL and authentication

- Local: `http://localhost:8080/`
- Deployed: use the configured HTTPS origin.
- Protected REST endpoints require `Authorization: Bearer <access-token>`.
- `/ws/events` requires the same bearer token in the WebSocket handshake Authorization header. Query-string tokens are not supported.
- Access tokens are short-lived. Refresh tokens are opaque, one-time rotating values; only their hashes are stored.

## Authentication endpoints

### POST `/api/v1/auth/login`

Request:

```json
{"username":"<provisioned-username>","password":"<password>"}
```

Response:

```json
{
  "token": "<access-token>",
  "refreshToken": "<opaque-refresh-token>",
  "expiresIn": 900,
  "userId": "<user-id>",
  "username": "<username>",
  "role": "ADMIN"
}
```

### POST `/api/v1/auth/refresh`

Request: `{"refreshToken":"<opaque-refresh-token>"}`

Returns a new access token and a new refresh token. The submitted refresh token is revoked and cannot be reused.

### POST `/api/v1/auth/logout`

Accepts an optional `{"refreshToken":"<opaque-refresh-token>"}` body. When supplied, revokes that refresh token even if the access token has expired. With no refresh token, a valid access token revokes all refresh sessions for that user. Returns `204 No Content`.

## Operational endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/incidents?page=0&size=50` | Paginated incident feed; optional `state`, `threatType`, `nodeId`, `from`, `to` filters |
| GET | `/api/v1/incidents/{id}` | Incident detail |
| POST | `/api/v1/incidents/{id}/verify` | Operator annotation and lifecycle update |
| GET | `/api/v1/incidents/{id}/events` | Incident audit events |
| GET | `/api/v1/nodes` | Node list |
| GET | `/api/v1/nodes/{id}` | Node details |
| GET | `/api/v1/dashboard` | Dashboard summary |
| GET | `/api/v1/autopilot/policy?nodeId={id}` | Read policy |
| PUT | `/api/v1/autopilot/policy?nodeId={id}` | Update policy; ADMIN only |
| GET | `/api/v1/evidence/{incidentId}/image` | Image media endpoint (returns 501 until storage provider is configured) |
| GET | `/api/v1/evidence/{incidentId}/video` | Video media endpoint (returns 501 until storage provider is configured) |
| GET | `/api/v1/evidence/{incidentId}/audio` | Audio media endpoint (returns 501 until storage provider is configured) |
| GET | `/api/v1/evidence/{incidentId}/thumbnail` | Thumbnail endpoint (returns 501 until storage provider is configured) |

Verification request:

```json
{"label":"TRUE_POSITIVE","notes":"Optional operator notes","confidence":0.95}
```

Allowed labels: `TRUE_POSITIVE`, `FALSE_POSITIVE`, `UNCERTAIN`.

Autopilot policy timestamps are serialized as `lastSyncTimestamp` to match the Android DTO contract. The feed response uses `content`, `page`, `size`, `totalElements`, and `totalPages`. Incident DTOs currently expose canonical flat backend fields such as `threatType`, `threatSeverity`, `latitude`, `longitude`, `createdAt`, and `updatedAt`; details include nested `detail.annotations`, `detail.responseEvents`, and `detail.evidence`.

## WebSocket event envelope

```json
{"type":"INCIDENT_UPDATED","timestamp":"2026-10-01T00:00:00Z","eventId":"<uuid>","payload":{"id":"<incident-id>","state":"VERIFIED"}}
```

Known event types include `INCIDENT_NEW`, `INCIDENT_UPDATED`, `NODE_STATUS_CHANGED`, and `SYSTEM_ALERT`. WebSocket channel subscription is not yet enforced; authenticated sessions currently receive broadcast events.

## Error format

Errors include HTTP status, a stable code, a safe message, request path and request ID. Do not rely on exception details being exposed to clients.

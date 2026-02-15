# Reliable Event Processing Service

Small Spring Boot service showcasing reliable event ingestion patterns for interviews:
- idempotent `POST /events` API
- retry with exponential backoff
- dead-letter queue (DLQ) persistence
- structured logs with request/correlation IDs
- Docker Compose (`app + postgres`)

## Stack
- Java 17
- Spring Boot 3
- Postgres + Flyway
- Docker Compose

## Run locally
```bash
docker compose up --build
```

Service:
- App: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

## API
### POST /events
Headers:
- `Idempotency-Key` (required)
- `X-Request-Id` (optional; auto-generated if missing)
- `X-Correlation-Id` (optional; auto-generated if missing)

Request body:
```json
{
  "payload": {
    "eventType": "ORDER_CREATED",
    "orderId": "o-123"
  }
}
```

Example:
```bash
curl -i -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: evt-101" \
  -H "X-Request-Id: req-101" \
  -H "X-Correlation-Id: corr-101" \
  -d "{\"payload\":{\"eventType\":\"ORDER_CREATED\",\"orderId\":\"o-123\"}}"
```
### Windows PowerShell (Invoke-RestMethod)

> Note: In Windows PowerShell, `curl` is often an alias for `Invoke-WebRequest`, so `curl -H ...` can fail.
> Use `Invoke-RestMethod` (recommended) or `curl.exe`.

### Happy path
```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/events" `
  -Headers @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = "evt-101"
  } `
  -Body '{"payload":{"eventType":"ORDER_CREATED","orderId":"o-123"}}'
```

### Idempotency behavior
- First request with a key creates and processes an event.
- Re-run the same request with the same key to see `duplicate: true`.

### Retry + DLQ behavior
- Processing retries with exponential backoff (`3` attempts by default).
- To simulate a failure and move to DLQ, send:
```json
{
  "payload": {
    "eventType": "ORDER_CREATED",
    "forceFail": true
  }
}
```
```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/events" `
  -Headers @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = "evt-fail-1"
  } `
  -Body '{"payload":{"eventType":"ORDER_CREATED","forceFail":true}}'
```

## Design notes
- In this MVP, retries are synchronous in the request path for simplicity.
- Production direction:
  - persist events as `RECEIVED`
  - process asynchronously via worker(s) + queue
  - schedule retries from DB metadata (`next_retry_at`) or broker delay queues
  - retain DLQ replay tooling


# Idempotent Order Processing System

A backend service demonstrating idempotency and distributed locking — solving a real production problem: preventing duplicate order creation when API requests are retried or sent concurrently.

## Problem

In distributed systems, network retries or duplicate client requests can cause the same operation to be processed multiple times. This project shows how to make an API **idempotent** and **race-condition safe**.

## Tech Stack

- Java 17, Spring Boot
- PostgreSQL (persistent storage)
- Redis (distributed locking)
- Docker

## How It Works

1. **Idempotency Check** — Before creating an order, the service checks if an order with the same `orderId` already exists. If yes, it returns the existing order instead of creating a duplicate.
2. **Distributed Locking** — Uses Redis's atomic `SETNX` (`setIfAbsent`) operation to ensure that even if two identical requests arrive at the exact same millisecond, only one is processed. The other receives a `409 Conflict`.

## Proof of Concurrency Safety

Two simultaneous requests for the same `orderId`, fired in parallel via shell:

```bash
curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"orderId":"ORD888","amount":300}' & curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"orderId":"ORD888","amount":300}' &
wait
```

**Result:**
- Request 1 → `200 OK` — order created
- Request 2 → `409 Conflict` — "Order is already being processed, try again"

This confirms no duplicate order was created under concurrent load.

## API

**POST /orders**

Request body:
```json
{
  "orderId": "ORD123",
  "amount": 500.0
}
```

Response:
```json
{
  "id": 1,
  "orderId": "ORD123",
  "amount": 500.0,
  "status": "CREATED",
  "createdAt": "2026-06-27T08:38:26"
}
```

## Run Locally

```bash
docker run --name order-postgres -e POSTGRES_PASSWORD=password123 -e POSTGRES_DB=orderdb -p 5432:5432 -d postgres
docker run --name order-redis -p 6379:6379 -d redis
./mvnw spring-boot:run
```

## What This Demonstrates

- Idempotent API design
- Distributed locking with Redis
- Handling race conditions in concurrent systems
- Clean separation of concerns (Controller, Repository, Entity)

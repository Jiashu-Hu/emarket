# emarket

A four-service microservices system (Account, Item, Order, Payment) on Spring Boot 4.0.5, Java 21, Maven.

Phase A delivers the **multi-module Maven skeleton and the Docker-compose infrastructure**. Each service is a runnable Spring Boot application with a `/health` endpoint and an `/actuator/health` endpoint — no business logic yet. Phases B–F add JWT auth, persistence, Kafka wiring, and hardening (see `docs/requirement.md`).

## Prerequisites

- **Docker + Docker Compose** — required for running the full stack.
- **JDK 21** — only if you want to build or run a service outside Docker. The Maven wrapper (`./mvnw`) handles Maven itself.

## Build & run

```bash
# 1. Build all four service jars
./mvnw clean verify

# 2. Build images and bring up the full stack (MySQL, MongoDB, Cassandra, Kafka, 4 services)
docker compose -f docker/docker-compose.yml up --build -d

# 3. Confirm each service is alive
curl -fsS localhost:8081/health   # {"service":"account","status":"UP"}
curl -fsS localhost:8082/health   # {"service":"item","status":"UP"}
curl -fsS localhost:8083/health   # {"service":"order","status":"UP"}
curl -fsS localhost:8084/health   # {"service":"payment","status":"UP"}

# 4. Tear down
docker compose -f docker/docker-compose.yml down -v
```

`docker compose up --build` requires the jars from step 1 to exist in `<service>/target/` — the Dockerfiles `COPY target/*.jar` rather than building inside the image.

## Port map

| Service          | Port | Data store | Messaging |
|------------------|------|------------|-----------|
| account-service  | 8081 | MySQL      | —         |
| item-service     | 8082 | MongoDB    | —         |
| order-service    | 8083 | Cassandra  | Kafka     |
| payment-service  | 8084 | MySQL      | Kafka     |
| MySQL            | 3306 | —          | —         |
| MongoDB          | 27017| —          | —         |
| Cassandra        | 9042 | —          | —         |
| Kafka            | 9092 | —          | —         |

## Layout

```
pom.xml                    parent POM (packaging=pom)
account-service/           :8081 — MySQL, auth server (Phase B)
item-service/              :8082 — MongoDB (Phase C)
order-service/             :8083 — Cassandra + Kafka (Phase D)
payment-service/           :8084 — MySQL + Kafka (Phase E)
docker/
  docker-compose.yml       full local stack
  mysql/init.sql           creates `account` and `payment` schemas
docs/
  requirement.md           full product spec
  Plan Phase A.md          this phase
  Plan Phase B/C/D/E/F.md  next phases
```

## What's in Phase A vs. next phases

Phase A intentionally contains **no business logic, no entities, no Kafka topics, no Spring Security**. Every service is a skeleton with two health endpoints and two tests. This lets each subsequent PR drop into a ready slot: Phase B adds JPA + JWT to account-service, Phase C adds the Mongo layer to item-service, and so on.

See `docs/Plan Phase A.md` for what was built and why, and `docs/Plan Phase B.md` onward for the roadmap.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is a Java 21, Maven, Spring Boot 4.0.5 backend project for an online shopping system. The repository is now a multi-module Maven build with four service modules:

- `account-service`
- `item-service`
- `order-service`
- `payment-service`

The current implementation is uneven by design:

- `account-service` has real application code for account registration, account lookup/update, password hashing, JWT issuance, JWKS exposure, Spring Security, JPA entities, Flyway migration, and service/repository tests.
- `item-service`, `order-service`, and `payment-service` are still mostly service skeletons with Spring Boot applications, `/health`, `/actuator/health`, and basic context/controller tests.
- Docker Compose already defines the required local infrastructure: MySQL, MongoDB, Cassandra, Kafka, and the four service containers.

Use `docs/requirement.md` as the product specification and `docs/Plan Phase *.md` as the staged roadmap, but verify against source because some older docs may describe an earlier scaffold state.

## Commands

Use the Maven wrapper from the repository root; do not assume a system `mvn` is installed.

```bash
./mvnw clean compile                 # compile all modules
./mvnw test                          # run all tests
./mvnw verify                        # run tests and generate JaCoCo reports
./mvnw package                       # build service jars into each module's target/
./mvnw -pl account-service test      # run tests for one module
./mvnw -pl account-service test -Dtest=AccountServiceTest
./mvnw -pl account-service test -Dtest=AccountServiceTest#registerPersistsBCryptedPassword
./mvnw -pl account-service spring-boot:run
```

There is no dedicated lint target in the current Maven build.

Docker images expect prebuilt jars because each service `Dockerfile` uses `COPY target/*.jar app.jar`.

```bash
./mvnw clean package
docker compose -f docker/docker-compose.yml up --build -d
docker compose -f docker/docker-compose.yml down -v
```

Health checks after the stack is up:

```bash
curl -fsS localhost:8081/health      # account-service
curl -fsS localhost:8082/health      # item-service
curl -fsS localhost:8083/health      # order-service
curl -fsS localhost:8084/health      # payment-service
```

## Architecture overview

The root `pom.xml` is a parent POM (`packaging=pom`) that aggregates the four service modules. Each service is intended to remain independently runnable and deployable even though they live in one repository today.

Target service responsibilities from `docs/requirement.md`:

- Account Service: users, addresses, payment methods, and authentication/token issuance; relational persistence via MySQL/PostgreSQL.
- Item Service: item metadata and inventory lookup/update; MongoDB persistence.
- Order Service: order lifecycle (`Created`, `Paid`, `Completed`, `Cancelled`), REST APIs, Kafka producer/consumer, Cassandra persistence.
- Payment Service: submit/update/reverse/lookup payments, idempotency guarantees, Kafka transaction result publishing, relational persistence.

Local infrastructure is defined in `docker/docker-compose.yml`:

- MySQL on `3306` for account and payment schemas.
- MongoDB on `27017` for item metadata/inventory.
- Cassandra on `9042` for orders.
- Kafka on `9092` for asynchronous order/payment communication.
- Service ports: account `8081`, item `8082`, order `8083`, payment `8084`.

## Current module details

### account-service

`account-service` is the active business module.

Important implemented flows:

- `POST /accounts` creates an account.
- `POST /auth/token` issues a Bearer JWT for valid credentials.
- `GET /accounts/me` returns the authenticated user.
- `PUT /accounts/me` updates the authenticated user profile.
- `/.well-known/jwks.json` exposes the public key set for JWT verification.

Persistence and security:

- JPA/Hibernate entities model users, embedded addresses, and payment methods.
- Flyway migration `V1__init.sql` creates `users` and `payment_methods` tables.
- Passwords are BCrypt-hashed.
- JWTs are RSA-signed with development keys under `account-service/src/main/resources/dev-keys/`.
- Spring Security permits account creation, token issuance, JWKS, Swagger, and health endpoints; other account endpoints require JWT authentication.

Tests cover service logic, JWT signing/parsing, repository behavior, health endpoint, and context startup.

### item-service

Currently a Spring Boot skeleton with `/health` and actuator health only. MongoDB dependencies, item documents, repositories, inventory logic, and service APIs are not implemented yet.

### order-service

Currently a Spring Boot skeleton with `/health` and actuator health only. Cassandra dependencies, order domain/state transitions, REST APIs, Kafka producers/consumers, and inter-service calls are not implemented yet.

### payment-service

Currently a Spring Boot skeleton with `/health` and actuator health only. Relational persistence, payment APIs, idempotency handling, and Kafka publishing/consumption are not implemented yet.

## Development notes

- Base Java package is `com.shopping.emarket`, with service-specific subpackages such as `com.shopping.emarket.account`.
- Keep service boundaries explicit; avoid sharing business logic directly across service modules unless a shared module is deliberately introduced.
- Add dependencies only to the service that needs them. For example, account already has JPA/Security/Flyway, but item/order/payment do not yet have their target persistence or Kafka dependencies.
- Use Swagger/OpenAPI support where service APIs are implemented; `account-service` already includes `springdoc-openapi-starter-webmvc-ui`.
- The required service-layer coverage target from the project spec is at least 30%; the parent POM configures JaCoCo reports during `verify`.

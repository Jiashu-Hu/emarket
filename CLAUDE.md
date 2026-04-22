# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repository is a **freshly-scaffolded Spring Boot 4.0.5 project** (Java 21, Maven). The only application code is `EmarketApplication.java` and the default context-load test. None of the business services, databases, messaging, or security layers described in `docs/requirement.md` have been implemented yet — treat that file as the specification for what needs to be built.

Known issue in the current scaffold: `src/main/java/com/shopping/emarket/EmarketApplication.java:9` contains a typo (`public tatic void main` — missing `s`). The project does not compile until this is fixed.

## Commands

Use the Maven wrapper — do not assume a system `mvn` is installed.

```bash
./mvnw clean compile            # compile
./mvnw test                     # run all tests
./mvnw test -Dtest=ClassName    # run one test class
./mvnw test -Dtest=ClassName#method   # run one test method
./mvnw spring-boot:run          # run the app locally
./mvnw package                  # build the jar into target/
```

## Target architecture (from `docs/requirement.md`)

The end state is a microservices system, not a monolith. When adding code, keep service boundaries clean so the current single-module layout can be split later (or introduce modules/sub-projects as soon as it makes sense).

Services to build:
- **Account Service** — users, addresses, payment methods. MySQL or PostgreSQL. May also host the auth server that issues tokens consumed by other services.
- **Item Service** — item metadata + inventory lookup/update. MongoDB (schema-flexible metadata).
- **Order Service** — order lifecycle (`Created → Paid → Completed → Cancelled`). Cassandra. Both REST and Kafka (producer *and* consumer).
- **Payment Service** — submit / update / reverse / lookup. **Idempotency is a hard requirement** — no double charges, no double refunds. Publishes transaction results.

Cross-cutting constraints:
- **All three databases must be used**: MySQL/PostgreSQL, MongoDB, Cassandra — assigned per service as above.
- **Kafka** for async/event-driven communication between services (notably order ↔ payment).
- **Spring Cloud OpenFeign and/or RestTemplate** for sync inter-service calls.
- **Auth server** issues tokens; downstream services accept requests only with a valid token in the header. Spring Security is the expected implementation.
- **Test coverage ≥ 30%** on each service layer (JUnit / Mockito / PowerMock, Jacoco for reporting).
- **Swagger** for API docs, **Spring Data JPA / Hibernate** for the relational service.
- Everything must be **dockerized and one-click runnable**, including dependencies (databases, Kafka).

## Conventions

- Base package: `com.shopping.emarket`.
- Java 21 — use modern language features (records, pattern matching, sealed types) where they clarify intent.
- Only `spring-boot-starter` and `spring-boot-starter-test` are currently on the classpath. Add dependencies deliberately as each service is introduced; don't pre-add the whole target stack.

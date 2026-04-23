# Plan: Account Service + Auth Server (Phase B)

## Context

Phase A (`docs/Plan Phase A.md`) lays out the multi-module restructure and
infra compose. Phase B is the first service to get real code. Two things
drive its scope:

1. `docs/requirement.md` says the Account service must let users create and
   update accounts holding email, username, password, shipping address,
   billing address, and payment method; MySQL/PostgreSQL for persistence.
2. The same spec requires an auth server that issues tokens consumed by the
   other three services. Phase A.md places that auth server inside
   `account-service`. Phase B must stand it up so Phases C–E can plug a JWT
   resource-server filter into Item/Order/Payment without any more auth work.

End state after this PR:
- `account-service` owns a MySQL schema with `users` + `payment_methods`,
  migrated by Flyway.
- `POST /accounts` (register), `GET/PUT /accounts/me` (authenticated).
- `POST /auth/token` with email + password returns a signed JWT; a JWKS
  endpoint publishes the public key so downstream services can verify
  tokens without calling back.
- Spring Security is configured as both issuer and resource-server for this
  service's own protected endpoints.
- Service-layer unit test coverage ≥ 30% (Jacoco module report).
- `docker compose up` still green; `account-service` reachable on `:8081`
  with working `/auth/token` and `/.well-known/jwks.json`.

**Prerequisite:** Phase A must land first (multi-module layout,
`account-service/` skeleton, `docker/docker-compose.yml`, MySQL `account`
schema in `docker/mysql/init.sql`). This plan edits files the Phase A PR
creates; it is not buildable against the current single-module tree.

## Shape of the change

```mermaid
graph LR
  subgraph AccountService["account-service (:8081)"]
    AuthCtl["AuthController<br/>POST /auth/token"]
    AcctCtl["AccountController<br/>POST /accounts<br/>GET/PUT /accounts/me"]
    JwksCtl["JwksController<br/>GET /.well-known/jwks.json"]

    AuthSvc["AuthService<br/>(verify password, mint JWT)"]
    AcctSvc["AccountService<br/>(CRUD + BCrypt)"]
    JwtSvc["JwtService<br/>(sign / parse / JWKS)"]

    SecCfg["SecurityConfig<br/>issuer + resource-server<br/>(local JWKS)"]
    KeyProv["JwtKeyProvider<br/>RSA PEM from env"]

    Repo["UserRepository<br/>(Spring Data JPA)"]

    AuthCtl --> AuthSvc --> JwtSvc
    AuthSvc --> Repo
    AcctCtl --> AcctSvc --> Repo
    JwksCtl --> JwtSvc
    JwtSvc --> KeyProv
    SecCfg -. verifies .-> JwtSvc
    SecCfg -. protects .-> AcctCtl
  end

  MySQL[(MySQL: account schema<br/>users, payment_methods)]
  Keys[[docker/account-keys/*.pem<br/>(dev RSA keypair)]]

  Repo --> MySQL
  KeyProv --> Keys

  subgraph Downstream["(future — not this PR)"]
    Item["item-service"]
    Order["order-service"]
    Payment["payment-service"]
  end
  Item -. fetches JWKS .-> JwksCtl
  Order -. fetches JWKS .-> JwksCtl
  Payment -. fetches JWKS .-> JwksCtl
```

Key design choices (picked so reviewers can see them up front):

- **JWT library:** `nimbus-jose-jwt` (transitive via
  `spring-security-oauth2-jose`). No Spring Authorization Server module —
  overkill for one `/auth/token` endpoint.
- **Key management:** a committed dev RSA keypair at
  `docker/account-keys/private_key.pem` + `public_key.pem`, clearly marked
  DEV ONLY in a README next to them. Container reads paths from
  `EMARKET_JWT_PRIVATE_KEY_PATH` / `EMARKET_JWT_PUBLIC_KEY_PATH`.
  Deterministic across restarts and reproducible in tests; prod override is
  a Phase F concern.
- **Account shape:** one `users` table with `User` entity embedding
  `shipping_*` and `billing_*` via a single `Address` `@Embeddable` reused
  twice; `payment_methods` is a separate `@OneToMany` table
  (`id, user_id, type, brand, last4, token_ref`). The spec hedges between
  singular and plural — one-to-many costs nothing now and avoids a
  migration later.
- **Password hashing:** BCrypt via `PasswordEncoder` bean.
- **Claims in the JWT:** `sub = user id (UUID string)`, `email`, `iss =
  http://account-service:8081`, `aud = emarket`, `exp = now + 1h`, `iat`,
  `kid` in header matching the JWKS key id. No refresh token in Phase B.

## Files to change

Paths assume the Phase A layout.

### 1. `account-service/pom.xml`
Add starters (versions already managed by the parent's Spring Boot BOM):
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`  *(pulls in nimbus-jose-jwt)*
- `spring-boot-starter-validation`
- `com.mysql:mysql-connector-j` (runtime)
- `org.flywaydb:flyway-core`, `org.flywaydb:flyway-mysql`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`
- test: `com.h2database:h2` (for `@DataJpaTest`),
  `org.springframework.security:spring-security-test`

### 2. `account-service/src/main/java/com/shopping/emarket/account/`
```
domain/
  User.java                 @Entity users: id (UUID), email (unique),
                            username (unique), passwordHash, Address shipping,
                            Address billing, @OneToMany paymentMethods,
                            createdAt, updatedAt
  Address.java              @Embeddable: line1, line2, city, region,
                            postalCode, country
  PaymentMethod.java        @Entity payment_methods: id (UUID), userId,
                            type (CARD/PAYPAL), brand, last4, tokenRef,
                            createdAt
repo/
  UserRepository.java       extends JpaRepository<User, UUID>;
                            findByEmail(String)
service/
  AccountService.java       register(CreateAccountRequest) →
                            validates email uniqueness, BCrypts password,
                            persists; update(UUID, UpdateAccountRequest);
                            findById(UUID); findByEmail(String)
  AuthService.java          issueToken(String email, String rawPassword) →
                            loads user, checks BCrypt, delegates to JwtService,
                            returns TokenResponse; throws BadCredentials
  JwtService.java           sign(Map<String,Object> claims) → String;
                            parse(String) → JWTClaimsSet;
                            jwks() → JWKSet (public only)
security/
  JwtKeyProvider.java       @Component reading PEMs via
                            @ConfigurationProperties(prefix="emarket.jwt");
                            exposes RSAKey (kid + public + private)
  SecurityConfig.java       SecurityFilterChain:
                            permitAll: POST /accounts, /auth/**,
                            /.well-known/jwks.json, /actuator/health,
                            /v3/api-docs/**, /swagger-ui/**;
                            everything else authenticated;
                            oauth2ResourceServer().jwt(jwt -> jwt
                              .decoder(NimbusJwtDecoder.withPublicKey(
                                keyProvider.rsa().toRSAPublicKey()).build()))
web/
  AccountController.java    POST /accounts (public, register);
                            GET /accounts/me; PUT /accounts/me
  AuthController.java       POST /auth/token {email, password}
                            → {accessToken, tokenType:"Bearer", expiresIn}
  JwksController.java       GET /.well-known/jwks.json →
                            jwtService.jwks().toJSONObject()
  GlobalExceptionHandler.java  @ControllerAdvice mapping
                            DuplicateEmailException → 409,
                            BadCredentialsException → 401,
                            EntityNotFoundException → 404
dto/
  CreateAccountRequest.java, UpdateAccountRequest.java,
  AccountResponse.java, TokenRequest.java, TokenResponse.java,
  PaymentMethodDto.java      (all records; bean-validation annotations)
```

### 3. `account-service/src/main/resources/`
- `application.yml` — replace the Phase A stub:
  ```yaml
  server.port: 8081
  spring:
    application.name: account-service
    datasource:
      url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/account}
      username: ${SPRING_DATASOURCE_USERNAME:root}
      password: ${SPRING_DATASOURCE_PASSWORD:root}
    jpa:
      hibernate.ddl-auto: validate
      open-in-view: false
    flyway.enabled: true
  emarket:
    jwt:
      issuer: http://account-service:8081
      audience: emarket
      key-id: emarket-dev-1
      private-key-path: ${EMARKET_JWT_PRIVATE_KEY_PATH:classpath:dev-keys/private_key.pem}
      public-key-path:  ${EMARKET_JWT_PUBLIC_KEY_PATH:classpath:dev-keys/public_key.pem}
      ttl: PT1H
  management.endpoints.web.exposure.include: health
  springdoc.swagger-ui.path: /swagger-ui.html
  ```
- `db/migration/V1__init.sql` — MySQL DDL for `users` (with embedded
  shipping/billing columns) and `payment_methods` (FK to users).
- `dev-keys/private_key.pem`, `dev-keys/public_key.pem` — 2048-bit RSA
  PKCS#8, committed for local/test use only, top-of-file comment says so.

### 4. `account-service/src/test/java/com/shopping/emarket/account/`
Cover the service layer to clear the 30% Jacoco bar; controllers
additionally to prove the security wiring works.
- `service/AccountServiceTest.java` — register happy path, duplicate email
  throws, update persists, BCrypt verified.
- `service/AuthServiceTest.java` — valid credentials → non-empty token,
  unknown email and wrong password both throw `BadCredentialsException`.
- `service/JwtServiceTest.java` — sign then parse roundtrip, claims match,
  `jwks()` output contains the expected `kid` and no private material.
- `security/JwtKeyProviderTest.java` — loads PEMs from classpath, exposes a
  matching RSA public/private pair.
- `web/AuthControllerTest.java` — `@WebMvcTest` + `MockMvc`, happy path
  returns 200 and a JWT; bad password returns 401.
- `web/AccountControllerTest.java` — `@WebMvcTest`: `POST /accounts`
  unauthenticated returns 201; `GET /accounts/me` without token returns 401;
  with a minted token returns 200.
- `repo/UserRepositoryTest.java` — `@DataJpaTest` with H2, `findByEmail`
  returns the inserted user; unique constraint violates on duplicate email.

### 5. `docker/docker-compose.yml` (edit the Phase A file)
Under the `account-service` entry add:
- `depends_on: { mysql: { condition: service_healthy } }` (Phase A already
  sets it but confirm).
- `environment:`
  ```
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/account
  SPRING_DATASOURCE_USERNAME: root
  SPRING_DATASOURCE_PASSWORD: root
  EMARKET_JWT_PRIVATE_KEY_PATH: /run/secrets/jwt/private_key.pem
  EMARKET_JWT_PUBLIC_KEY_PATH:  /run/secrets/jwt/public_key.pem
  ```
- `volumes: [ "./account-keys:/run/secrets/jwt:ro" ]`.

### 6. `docker/account-keys/`
- `private_key.pem`, `public_key.pem` — dev keypair, same material shipped
  inside the jar's `dev-keys/` classpath resources so unit tests work out of
  the box. `README.md` in the folder says "DEV ONLY — rotate in prod".

### 7. `docker/mysql/init.sql` (edit)
Confirm Phase A already creates the `account` schema. No change expected;
Flyway owns table DDL.

### 8. Root `README.md` (append)
Short "Auth quickstart":
```
curl -sX POST localhost:8081/accounts -H 'content-type: application/json' \
  -d '{"email":"a@b.c","username":"a","password":"Passw0rd!"}'
TOKEN=$(curl -sX POST localhost:8081/auth/token -H 'content-type: application/json' \
  -d '{"email":"a@b.c","password":"Passw0rd!"}' | jq -r .accessToken)
curl -sH "authorization: Bearer $TOKEN" localhost:8081/accounts/me
curl -s localhost:8081/.well-known/jwks.json | jq .
```

## Execution order

1. Add starters to `account-service/pom.xml`; run `./mvnw -pl account-service
   dependency:tree` to confirm nimbus-jose-jwt is on the path.
2. Generate the dev RSA keypair (`openssl genpkey` + `openssl rsa -pubout`),
   copy the same files into `account-service/src/main/resources/dev-keys/`
   and `docker/account-keys/`.
3. Write `JwtKeyProvider` + `JwtService` + a `JwtServiceTest` roundtrip —
   get this green before anything touches the DB.
4. Write `V1__init.sql` + entities + `UserRepository` + `UserRepositoryTest`
   (`@DataJpaTest` on H2 with MySQL-compat dialect).
5. Write `AccountService` + `AuthService` + their unit tests.
6. Write `SecurityConfig`, the three controllers, `GlobalExceptionHandler`,
   DTOs, and the `@WebMvcTest`s.
7. `./mvnw -pl account-service clean verify` — green, Jacoco ≥ 30% on the
   `service` package.
8. Wire compose env + keys volume; `docker compose up --build account-service
   mysql -d`; run the README curl flow end-to-end.
9. Open the PR.

## Verification

- `./mvnw -pl account-service clean verify` passes; the Jacoco HTML report
  at `account-service/target/site/jacoco/index.html` shows `service/` ≥ 30%.
- `docker compose up --build -d` then, from the host:
  - `curl -fsS localhost:8081/actuator/health` → `{"status":"UP"}`.
  - Registering, logging in, and `GET /accounts/me` with the returned token
    all succeed (README quickstart commands).
  - `GET /.well-known/jwks.json` returns a JWKS with one RSA key,
    `kid=emarket-dev-1`, and no `d`/`p`/`q` fields (public-only).
  - Decode the JWT at jwt.io with the public key → signature verifies; `iss`,
    `aud`, `sub`, `exp` match config.
  - `docker compose exec mysql mysql -uroot -proot account -e 'select email
    from users;'` shows the registered user.
- Negative checks: `GET /accounts/me` without a bearer returns 401; with a
  token whose signature was tampered returns 401; duplicate registration
  returns 409.

## Out of scope (explicitly deferred)

- Cross-service JWT verification filter in Item/Order/Payment — Phase C
  introduces the first consumer and therefore owns that work.
- Refresh tokens, password reset, email verification, rate limiting — Phase
  F hardening.
- Admin endpoints / role-based authorization — no requirement calls for it
  yet; Phase B uses a single `ROLE_USER` implicitly.
- Production key rotation / secrets management (Vault, KMS) — Phase F.
- Account deletion and GDPR export — not in the spec.
- Payment method CRUD endpoints — the table exists so Phase E can link to
  it, but no `/accounts/me/payment-methods` API in this PR.
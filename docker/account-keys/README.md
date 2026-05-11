# dev RSA keypair — DEV ONLY

These files are a **deterministic, committed development keypair** used for local and CI runs of `account-service`. They are **not** a production secret.

- `private_key.pem` — 2048-bit RSA, PKCS#8.
- `public_key.pem` — matching SPKI public key.

## Why committed?

Phase B values reproducibility: every developer, every `docker compose up`, every CI run needs the same JWT signature to produce predictable tests and demo flows. Rotating per-environment keys is deferred to Phase F.

## Production

Do **not** deploy these files. In Phase F the container will read keys from a real secret source (Vault, cloud KMS, mounted secret). Override the container env vars:

- `EMARKET_JWT_PRIVATE_KEY_PATH`
- `EMARKET_JWT_PUBLIC_KEY_PATH`

## Regenerating

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
openssl rsa -in private_key.pem -pubout -out public_key.pem
```

Copy the same two files into `account-service/src/main/resources/dev-keys/` so tests keep using the same material as the container.

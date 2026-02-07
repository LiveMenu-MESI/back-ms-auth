# liveMenu

This project uses Quarkus, the Supersonic Subatomic Java Framework.

## Secrets

All secrets are provided via environment variables or a `.env` file (never committed). Copy `.env.example` to `.env` and set values; for Docker Compose use `compose/.env`. Realm client secrets, DB passwords, and Keycloak admin credentials are read from env; the realm import JSON and init scripts use placeholders or env only.

## Auth (Keycloak)

Register uses the Keycloak admin user (master realm): set `KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD` in `.env` (same as in `compose/.env`). Start infrastructure: `cd compose && docker compose up -d`. Wait for Keycloak (~50s), then run the app: `./mvnw quarkus:dev`. Test: Postman collection in `postman/Auth-LiveMenu.postman_collection.json` (Register → Login → Get current user → Logout). If login returns "Invalid credentials" or "Account is not fully set up", reset Keycloak so the realm re-imports: `cd compose && docker compose down -v && docker compose up -d`, then register a new user and try login again.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/liveMenu-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

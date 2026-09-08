# Container Delivery

[English](README.md) | [中文](README.zh-CN.md)

## One Portable Dockerfile

`deploy/container/Dockerfile` is the only image build definition. Docker, Podman,
and nerdctl/BuildKit consume the same standards-oriented multi-stage file. The
`CONTAINER_ENGINE` build argument records which command performed the build; it
does not select a second Dockerfile.

```bash
IMAGE_NAME="$(bash ./mvnw -q -DforceStdout help:evaluate -Dexpression=project.artifactId | tail -n 1)"
docker build --build-arg CONTAINER_ENGINE=docker \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
podman build --build-arg CONTAINER_ENGINE=podman \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
```

The Dockerfile packages source with the Maven Wrapper. All Maven dependencies,
including organization-specific Facade artifacts, must be resolvable from the
build environment. Private-repository credential transport is an operator concern
and must not be encoded as a Docker build argument because build arguments are not
secret storage.

## Docker

Docker requires a reachable daemon and the Compose v2 plugin.

## Podman

Podman may run rootless or rootful. `podman compose` also requires a configured
Compose provider. Inspect `podman compose version` before using the generated
commands.

## nerdctl

nerdctl requires reachable containerd and BuildKit services. Select the intended
containerd namespace in the command or Jenkins parameter; `default` is the
generated Jenkins default.

## Rootless And Rootful

The application runs as a numeric non-root user. Compose files avoid privileged
mode, host networking, runtime sockets, fixed host data paths, and ports below
1024. Infrastructure images still require host user-namespace and volume support
appropriate to the selected engine.

## Development Compose

Development definitions build source and start the application, three PostgreSQL
primaries, Redis, RabbitMQ. Use the file matching the selected runtime:

```bash
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
podman compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.podman.yaml up -d --build
nerdctl compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.nerdctl.yaml up -d --build
```

The bundled Compose files set `APP_DATASOURCE_MODE=SHARDING` and provision
`master_data`, `shard_0`, and `shard_1` primaries. They intentionally do not
emulate replicas. `SHARDING_READWRITE` requires an operator-managed topology and
all variables declared by `datasource/sharding-readwrite.yml`.

The example credentials are development-only.

## Production Compose

Copy `deploy/env/.env.prod.example` to an ignored operator-owned `.env.prod`, set
every blank required value, and use the production file for the selected runtime.
Production files pull `${REGISTRY}/${REGISTRY_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}`
and never build source on the server.

The generated production topology is a single-host baseline. It does not provide
high availability, backup, certificate issuance, cross-host scheduling, or
disaster recovery. Replace included infrastructure endpoints with managed or
clustered services when those properties are required.

## Persistent Data

Ordinary `stop` and `down` retain named volumes. A command that explicitly removes
volumes permanently deletes local database, broker, cache, and application
log data. No generated helper performs that deletion automatically.

## Health And Failure Behavior

All three PostgreSQL primaries, Redis, RabbitMQ, and the Spring Boot
readiness endpoint have health checks. Missing production variables fail Compose configuration. An enabled
but unavailable remote Facade retains the generated application's fail-fast
behavior.

## Jenkins

The root `Jenkinsfile` tests, builds, and optionally publishes the image with
Docker, Podman, or nerdctl. It never runs Compose or deploys the project.
`PUBLISH_IMAGE` and `PUBLISH_LATEST` default to `false`; registry publication must
be explicitly enabled with Jenkins credentials.

## Native RPC, DDC and remote queries

This project exposes 10 organization unary operations from the shared Protobuf contract. Providers delegate to the existing facades. Remote queries use the existing domain port, MapStruct/BaseConverter and component DIRECT proxy/strategy factories. Configure `organization.integrations.evaluation` with the exact target biz code, app code, group/version and timeout. `EVALUATION_FACADE_APP_CODE` must match the peer's registered DDC app code. References use the current process environment, version `1.0` by default, a 3000ms default bounded by the component ceiling, zero retries and FAIL_CLOSED.

Supply existing DDC RPC/Redis endpoints, registration resource URI, separate runtime/registry HMAC credentials, and an IdP SERVICE-token client allowed `ddc:registration:write`. Complete the `DDC_*`, `IDP_*` and advertised-host entries in the environment sample. Compose maps Spring OAuth2 Client registration/provider `ddcregistration`; direct Java launches must supply the corresponding `spring.security.oauth2.client.registration.ddcregistration` and `spring.security.oauth2.client.provider.ddcregistration.token-uri` external properties. Production enables RPC/DDC mTLS; configure and mount the certificate-chain, private-key and trust-certificate paths. No DDC or IdP container is bundled.

The platform OpenAPI MVC starter preserves business HTTP access. Document governance is disabled by default. Enabling it requires the platform document identity, published groups, JWT decoder and `gateway.openapi.read` scope; published handlers also require explicit `@Operation(operationId = "...")` metadata. IdP Servlet filter auto-registration is disabled. HTTP registration follows the effective `server.port`.

The `test` profile disables RPC provider/consumer, DDC config/registry/Redis, HTTP registration and remote query clients, retaining H2 and local stubs. Stock Redisson auto-configuration is excluded; DDC creates only its explicitly configured Redis client. Existing PostgreSQL, Redis, RabbitMQ and their data volumes remain. Configuration decryption runs after Spring Boot Config Data; explicit imports/configtree replace retired bootstrap loading while encryption and key rules remain unchanged. Static, module and in-process RPC tests do not establish live DDC/IdP, mTLS or container interoperability.

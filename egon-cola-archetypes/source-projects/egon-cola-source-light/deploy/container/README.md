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
primaries, Redis, and RabbitMQ. Use the file matching the selected runtime:

```bash
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
podman compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.podman.yaml up -d --build
nerdctl compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.nerdctl.yaml up -d --build
```

The bundled Compose files set `APP_DATASOURCE_MODE=SHARDING` and provision
`postgres-master-data`, `postgres-shard-0`, and `postgres-shard-1`. They do not
pretend to provide replication. `SHARDING_READWRITE` requires operator-provided
primary/replica endpoints for every logical group declared by
`datasource/sharding-readwrite.yml`.

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

## Native RPC and Tianshu configuration

All 10 unary operations are declared in `src/main/proto/teaching_user_facade.proto` and implemented by the four named RPC providers. The platform OpenAPI MVC starter supplies `/v3/api-docs`; existing business HTTP access is preserved. Governance is disabled by default. Enabling `egon.cola.component.yuheng.openapi.enabled` requires the platform document identity, published groups and JWT decoder settings, including `yuheng.openapi.read` scope. Every published handler also needs an explicit `@Operation(operationId = "...")`; the existing sample business controllers must be catalogued before enabling governance.

`dev` and `prod` require an existing Tianshu RPC endpoint (`TIANSHU_RPC_TARGET`), Tianshu Redis endpoint, registration resource URI, and separate runtime/registry HMAC credentials. RPC/Tianshu configuration uses `egon.cola.component.rpc` and `egon.cola.component.tianshu`; application identity uses `APP_NAME`, `APP_ENV`, `TIANSHU_BIZ_CODE`, `TIANSHU_APP_CODE`, and `INSTANCE_ID`. Configure advertised hosts reachable by consumers. No Tianshu container is supplied. The `test` profile disables RPC provider/consumer, Tianshu config/registry/Redis, HTTP registration and document publication.

Production enables RPC and Tianshu mTLS: supply certificate-chain, private-key and trust-certificate paths through the corresponding `RPC_*` and `TIANSHU_RPC_*` variables and mount those files in the container at the configured paths. The development profile explicitly permits plaintext. Configure deployment secrets outside source control; fill the blank values in `deploy/env/.env.prod.example` before deployment. Compose retains PostgreSQL, Redis, RabbitMQ and their data volumes. Static and module tests do not establish live Tianshu registration, TLS interoperability or container readiness.

Tianshu provider and HTTP registration additionally obtain an Tianquan-Shoubing SERVICE token with `tianshu:registration:write`. Fill the `TIANQUAN_SHOUBING_*` entries in the environment sample and configure Spring OAuth2 Client registration/provider `tianshuregistration` (`client_credentials`, `client_secret_basic`, client ID/secret and token URI). Compose maps those values to Spring's standard environment variables. For a direct Java launch, supply the equivalent `spring.security.oauth2.client.registration.tianshuregistration` and `spring.security.oauth2.client.provider.tianshuregistration.token-uri` properties through external configuration. The referenced app ID, resource server ID/URI and registration resource URI must match the existing Tianquan-Shoubing/Tianshu deployment. Tianquan-Shoubing's automatic Servlet filter is disabled to preserve current business HTTP access. The optional OpenAPI security chain still governs documents when explicitly enabled.

The stock Redisson auto-configuration is excluded; Tianshu owns its explicitly configured Redis client, while existing business Redis configuration remains on Spring's original connection factory. This prevents a disabled Tianshu/test profile from silently creating a Redis connection.

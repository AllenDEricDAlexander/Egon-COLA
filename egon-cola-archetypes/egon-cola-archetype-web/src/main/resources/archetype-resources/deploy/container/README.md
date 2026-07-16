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

Development definitions build source and start application, PostgreSQL, Redis,
RabbitMQ, and Nacos. Use the file matching the selected runtime:

```bash
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
podman compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.podman.yaml up -d --build
nerdctl compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.nerdctl.yaml up -d --build
```

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
volumes permanently deletes local database, broker, cache, Nacos, and application
log data. No generated helper performs that deletion automatically.

## Health And Failure Behavior

PostgreSQL, Redis, RabbitMQ, Nacos, and the Spring Boot readiness endpoint have
health checks. Missing production variables fail Compose configuration. An enabled
but unavailable remote Facade retains the generated application's fail-fast
behavior.

## Jenkins

The root `Jenkinsfile` tests, builds, and optionally publishes the image with
Docker, Podman, or nerdctl. It never runs Compose or deploys the project.
`PUBLISH_IMAGE` and `PUBLISH_LATEST` default to `false`; registry publication must
be explicitly enabled with Jenkins credentials.

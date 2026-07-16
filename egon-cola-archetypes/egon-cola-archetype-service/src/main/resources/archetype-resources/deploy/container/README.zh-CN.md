# 容器交付

[English](README.md) | 中文

## 一个可移植的 Dockerfile

`deploy/container/Dockerfile` 是唯一的镜像构建定义。Docker、Podman 和 nerdctl/BuildKit 使用同一个遵循标准的多阶段文件。`CONTAINER_ENGINE` build argument 记录执行构建的命令；它不会选择第二个 Dockerfile。

```bash
IMAGE_NAME="$(bash ./mvnw -q -DforceStdout help:evaluate -Dexpression=project.artifactId | tail -n 1)"
docker build --build-arg CONTAINER_ENGINE=docker \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
podman build --build-arg CONTAINER_ENGINE=podman \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl \
  --file deploy/container/Dockerfile --tag "$IMAGE_NAME:local" .
```

Dockerfile 使用 Maven Wrapper 打包源码。所有 Maven 依赖（包括组织专用的 Facade artifact）都必须能从构建环境解析。私有仓库凭据传输属于运维职责，不得编码为 Docker build argument，因为 build argument 不是密钥存储。

## Docker

Docker 需要可访问的 daemon 和 Compose v2 plugin。

## Podman

Podman 可以 rootless 或 rootful 运行。`podman compose` 还需要已配置的 Compose provider。使用生成的命令前，请先检查 `podman compose version`。

## nerdctl

nerdctl 需要可访问的 containerd 和 BuildKit 服务。请在命令或 Jenkins 参数中选择目标 containerd namespace；生成的 Jenkins 默认值是 `default`。

## Rootless 与 Rootful

应用以数字形式的非 root 用户运行。Compose 文件避免 privileged 模式、host network、运行时 socket、固定的主机数据路径和低于 1024 的端口。基础设施镜像仍然需要与所选引擎匹配的主机 user namespace 和 volume 支持。

## 开发 Compose

开发定义会构建源码并启动应用、PostgreSQL、Redis、RabbitMQ 和 Nacos。请使用与所选运行时匹配的文件：

```bash
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
podman compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.podman.yaml up -d --build
nerdctl compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.nerdctl.yaml up -d --build
```

示例凭据仅供开发使用。

## 生产 Compose

将 `deploy/env/.env.prod.example` 复制为被忽略、由运维方持有的 `.env.prod`，填写所有必需的空值，然后使用所选运行时对应的生产文件。生产文件拉取 `${REGISTRY}/${REGISTRY_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}`，不会在服务器上构建源码。

生成的生产拓扑是单主机基线，不提供高可用、备份、证书签发、跨主机调度或灾难恢复。需要这些能力时，应将内置基础设施端点替换为托管或集群化服务。

## 持久化数据

普通的 `stop` 和 `down` 会保留命名 volume。显式删除 volume 的命令会永久删除本地数据库、broker、缓存、Nacos 和应用日志数据。生成的辅助命令不会自动执行删除操作。

## 健康检查与失败行为

PostgreSQL、Redis、RabbitMQ、Nacos 和 Spring Boot readiness endpoint 都配置了 health check。缺少生产变量会导致 Compose 配置失败。已启用但不可用的远程 Facade 会保留生成应用的 fail-fast 行为。

## Jenkins

根目录 `Jenkinsfile` 使用 Docker、Podman 或 nerdctl 测试、构建并按需发布镜像。它不会运行 Compose，也不会部署项目。`PUBLISH_IMAGE` 和 `PUBLISH_LATEST` 默认都是 `false`；必须通过 Jenkins 凭据显式启用 registry 发布。

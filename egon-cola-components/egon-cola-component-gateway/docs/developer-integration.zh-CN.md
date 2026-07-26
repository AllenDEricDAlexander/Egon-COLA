# Gateway、DDC 与 RPC 开发联调 Runbook

[English](developer-integration.md) | [Gateway 概览](../README.zh-CN.md)

本文给出本地完整链路：Gateway Admin 通过 DDC 发布规则，两个 Gateway Engine
订阅规则和 Provider 租约，MVC/WebFlux/RPC Provider 注册并上报接口，RPC Consumer
只发现内部 Gateway。Nginx、生产 HA、外部 IAM 与生产 TLS 证书不属于该 Demo。

## 前置与证据边界

需要 JDK 21、Docker Compose、`curl`、`jq`、`openssl`，以及可用的 Maven Wrapper。
复制环境文件并替换所有示例密钥：

```bash
cd egon-cola-components/egon-cola-component-gateway/deployment
cp .env.example .env
chmod 600 .env
./scripts/demo.sh doctor
```

本仓库默认测试不会启动 Docker。`demo.sh --help`、Shell 语法、伪 Docker 安全测试和
`docker compose config --quiet` 可以作为静态证据；只有操作者实际完成下方生命周期，
才能声称 PostgreSQL、双 Redis、Kafka、双 Engine 和真实 Provider 链路通过。

## 拓扑与端口

```text
Admin Web :18090 -> Gateway Admin :18080 -> DDC Admin :18070
                                      |          |
                                      |          +-> DDC Redis
                                      +-> PostgreSQL + Kafka
Engine 1 :18081/:18082 RPC :19090 ----+----> Provider registry/rules
Engine 2 :18181/:18182 RPC :19190 ----+
MVC :18084  WebFlux :18085  RPC Provider :18086/:19091
RPC Consumer :18087 -> DDC discovers Engine RPC slots -> RPC Provider
```

DDC Redis 与分布式限流 Redis 是不同服务和数据卷；两个 Engine 使用不同 LKG 卷。

## 一条命令化生命周期

严格按顺序执行：

```bash
./scripts/demo.sh build
./scripts/demo.sh up-control
./scripts/demo.sh init
./scripts/demo.sh up-providers
./scripts/demo.sh publish
./scripts/demo.sh up-consumer
./scripts/demo.sh verify
./scripts/demo.sh logs
./scripts/demo.sh down
```

`init` 生成 12 小时本地 JWT，创建 HTTP/RPC Application、上报凭据和 Gateway Group。
JWT、凭据与对象 ID 只写入忽略目录 `.demo/`，文件权限为 0600。`publish` 根据实际上报
的 `methodIdentity` 解析 operationId，发布 HTTP Provider route、HTTP→RPC route 和
分布式限流策略，再等待双 Engine 一致。不要手工把 operationId 写入 fixture。

`down` 不删卷。确需删除本 Demo 的 PostgreSQL/Redis/Kafka/LKG 数据时才执行：

```bash
./scripts/demo.sh purge
```

`purge` 需要 `.demo/.local-demo-marker` 且 Compose project name 必须以
`egon-cola-gateway-demo-` 开头。它不可恢复。

## 手工成功判据

```bash
curl -fsS http://127.0.0.1:18070/api/v1/ddc/manifest
curl -fsS http://127.0.0.1:18080/actuator/health/readiness
curl -fsS -H 'Host: providers.gateway.demo' \
  http://127.0.0.1:18081/api/providers/manual-1 | jq
curl -fsS 'http://127.0.0.1:18087/test/rpc/echo?message=manual-rpc' | jq
```

重复第一个数据面请求应能观察到 `framework=mvc` 和 `framework=webflux`。RPC 返回应
包含消息、traceId 和 `rpc-provider-demo`。`.demo/admin.jwt` 可用于查询：

```bash
TOKEN="$(cat .demo/admin.jwt)"
GROUP_ID="$(cat .demo/group.id)"
curl -fsS -H "Authorization: Bearer ${TOKEN}" \
  "http://127.0.0.1:18080/api/v1/gateway/admin/gateway-groups/${GROUP_ID}/runtime-consistency" | jq
```

成功状态要求 `consistent=true`、`readyEngineNodeCount=2`，Provider 投影包含两个 HTTP
实例和 RPC 实例，Trace 投影包含 protocol、providerService 与 engineInstanceId。

## 故障演练

- `docker compose ... stop http-provider-mvc`：优雅注销后 WebFlux 应继续服务；重启后
  同一 instanceId 获得新 leaseId。
- `docker compose ... kill http-provider-webflux`：在租约 TTL 后摘除；MVC 继续服务。
- 停止一个 Engine：RPC Consumer 应在 DDC 更新后选择另一个 Gateway Slot；重启节点
  后获取新租约并重新进入轮转。
- 暂停 DDC：已 Ready Engine 只能继续有效内存规则/LKG；冷启动 Engine 不得 Ready。
- 暂停 Kafka：业务响应不得被改变，但 Kafka 发送失败/丢弃指标必须增加。

故障后重新执行 `publish` 或 `verify` 前，先看 `.demo/logs/compose.log`、Admin 的
runtime-consistency、Provider/Engine 投影和 Actuator readiness。不要用固定 sleep
代替状态检查。

## 自动化与未验证边界

默认门禁：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am test
```

真实 Testcontainers 门禁（会启动容器与多个 JVM）：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live verify
```

基础 Demo 不验证 Redis Sentinel/Cluster、PostgreSQL/Kafka HA、控制面多实例故障转移、
生产 TLS/mTLS、证书轮换、外部负载均衡或 Kubernetes。对应 overlay 只能证明配置可渲染，
实际运行前仍需在目标环境验证。

# Gateway 性能、长稳与故障演练

本目录提供可复现的测试入口，不包含未经测量的性能承诺。`capacity.json` 中
`PROVISIONAL_UNMEASURED` 表示阈值是发布候选门槛；只有在指定参考机器、数据规模和
拓扑上实际执行并归档结果后，才能把状态更新为已测量基线。

## 前置拓扑与场景

先由操作者启动并初始化真实拓扑，使以下路由可调用：

- HTTP→HTTP：PUBLIC `/api/orders/{id}`；
- HTTP→HTTP：INTERNAL `/api/internal/inventory/{sku}`；
- RPC→RPC：RPC Consumer 的 `/test/rpc/echo` 驱动接口；
- HTTP→RPC：INTERNAL `/rpc/echo`。

测试目标通过以下环境变量覆盖：

```text
GATEWAY_PUBLIC_BASE_URL
GATEWAY_INTERNAL_BASE_URL
GATEWAY_RPC_CONSUMER_BASE_URL
GATEWAY_PUBLIC_HOST
GATEWAY_INTERNAL_HOST
GATEWAY_RPC_HOST
```

执行 smoke 或固定基线：

```bash
GATEWAY_PERF_PROFILE=smoke ./run-k6.sh http
GATEWAY_PERF_PROFILE=smoke ./run-k6.sh rpc
GATEWAY_PERF_PROFILE=baseline ./run-k6.sh http
GATEWAY_PERF_PROFILE=baseline ./run-k6.sh rpc
```

脚本优先使用本机 k6，否则使用固定版本的 `grafana/k6` 镜像。吞吐、VUS 和时长可由
`GATEWAY_PERF_RATE`、`GATEWAY_PERF_PRE_ALLOCATED_VUS`、
`GATEWAY_PERF_MAX_VUS`、`GATEWAY_PERF_DURATION` 覆盖。结果默认进入
`artifacts/*-summary.json`。容量文件中的 Rate 是每个 Scenario 的速率；HTTP/RPC
脚本各含两个并行 Scenario，因此各自总到达率是该值的两倍。

## 24 小时长稳

```bash
GATEWAY_SOAK_DURATION_SECONDS=86400 ./soak.sh
```

长稳期间 `sample-resources.sh` 每 10 秒采集容器 CPU、Memory、Network、Block IO
和 PID 数，写入 `artifacts/resources.csv`。运行后还应从 Prometheus 归档 JVM
Heap/Direct Memory/GC、EventLoop、Connection/Channel、Inflight/Queue、Kafka Drop
和规则版本切换指标。`soak.sh` 顺序执行 HTTP 与 RPC，各自使用所给持续时间；完整
验收因此需要约 48 小时。若要求总计 24 小时，可分别设为 12 小时。

## 故障演练

脚本只接受固定故障目标，并在退出或中断时恢复被暂停的服务：

```bash
./faults.sh kafka 30
./faults.sh redis 30
GATEWAY_FAULT_REDIS_SERVICE=ddc-redis ./faults.sh redis 30
./faults.sh postgres 30
GATEWAY_PROVIDER_CONTAINER=gateway-test-http-provider ./faults.sh provider 30
```

演练前必须存在 `deployment/.env` 且 Compose 拓扑已由操作者启动。每次演练将开始、
恢复时间、前后状态及目标日志写入 `artifacts/`。验收时分别确认：

- Kafka 暂停不改变业务响应，失败/丢弃指标可见；
- 限流 Redis 按规则固定失败模式，DDC Redis 恢复后客户端重新收敛；
- PostgreSQL 暂停时读取面继续工作，管理写入明确失败且恢复后可重试；
- 一个 Provider 暂停后从候选摘除，其他实例继续提供服务。

所有结果必须连同 Git Commit、机器规格、JVM 参数、容器镜像 Digest、规则/Provider
数据规模和测试时长一起归档，才能用于版本间对比。

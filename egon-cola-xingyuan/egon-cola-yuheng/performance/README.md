# Yuheng Performance, Soak, and Fault Drills

[中文](README.zh-CN.md) | [Yuheng overview](../README.md)

This directory provides reproducible test entry points and makes no unmeasured performance
commitments. `capacity.json` uses `PROVISIONAL_UNMEASURED` to mark release-candidate gates;
the state may be changed to a measured baseline only after results are executed and archived
on specified reference hardware, data volume, and topology.

## Topology and scenarios

Start and initialize a real topology so that the following routes are callable:

- HTTP→HTTP: PUBLIC `/api/orders/{id}`;
- HTTP→HTTP: INTERNAL `/api/internal/inventory/{sku}`;
- RPC→RPC: RPC Consumer `/test/rpc/echo` driver endpoint;
- HTTP→RPC: INTERNAL `/rpc/echo`.

Override the test targets with:

```text
YUHENG_PUBLIC_BASE_URL
YUHENG_INTERNAL_BASE_URL
YUHENG_RPC_CONSUMER_BASE_URL
YUHENG_PUBLIC_HOST
YUHENG_INTERNAL_HOST
YUHENG_RPC_HOST
```

Run smoke or fixed-baseline profiles:

```bash
YUHENG_PERF_PROFILE=smoke ./run-k6.sh http
YUHENG_PERF_PROFILE=smoke ./run-k6.sh rpc
YUHENG_PERF_PROFILE=baseline ./run-k6.sh http
YUHENG_PERF_PROFILE=baseline ./run-k6.sh rpc
```

The scripts prefer a local k6 binary and otherwise use a pinned `grafana/k6` image. Throughput,
VUS, and duration can be overridden with `YUHENG_PERF_RATE`,
`YUHENG_PERF_PRE_ALLOCATED_VUS`, `YUHENG_PERF_MAX_VUS`, and `YUHENG_PERF_DURATION`.
Results are written to `artifacts/*-summary.json` by default. The Rate in the capacity file is
per Scenario; both the HTTP and RPC scripts contain two parallel Scenarios, so each script's
total arrival rate is twice the configured value.

## 24-hour soak

```bash
YUHENG_SOAK_DURATION_SECONDS=86400 ./soak.sh
```

During the soak, `sample-resources.sh` samples container CPU, Memory, Network, Block IO, and
PID count every 10 seconds into `artifacts/resources.csv`. Also archive JVM Heap/Direct Memory/
GC, EventLoop, Connection/Channel, Inflight/Queue, Kafka Drop, and rule-version-switch metrics
from Prometheus after the run. `soak.sh` runs HTTP and RPC sequentially, each for the supplied
duration; a complete acceptance run therefore takes about 48 hours. For a 24-hour total, use
12 hours for each scenario.

## Fault drills

The script accepts only fixed fault targets and restores paused services on exit or interruption:

```bash
./faults.sh kafka 30
./faults.sh redis 30
YUHENG_FAULT_REDIS_SERVICE=tianshu-redis ./faults.sh redis 30
./faults.sh postgres 30
YUHENG_PROVIDER_CONTAINER=yuheng-test-http-provider ./faults.sh provider 30
```

Before a drill, `deployment/.env` must exist and the Compose topology must already be running.
Each drill writes start/recovery times, before/after status, and target logs to `artifacts/`.
Acceptance checks are:

- pausing Kafka does not change the business response, while failure/drop metrics remain visible;
- rate-limit Redis follows the fixed failure mode, and the client converges again after Tianshu Redis recovers;
- while PostgreSQL is paused, reads continue, management writes fail explicitly, and writes can be retried after recovery;
- after pausing one Provider, it is removed from candidates while the remaining instances continue serving.

Archive every result with the Git commit, machine specification, JVM parameters, container image
digests, rule/Provider data volume, and test duration before using it for version-to-version
comparisons.

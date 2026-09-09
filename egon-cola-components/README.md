# Egon-COLA Components

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-components` is the Maven reactor for reusable Java libraries and Spring Boot starters. Each runtime capability is independently testable and published through the [Components BOM](egon-cola-components-bom/README.md).

## Component entry points

| Component | Capability | Consumer documentation |
|---|---|---|
| Agent Flow | Flat configuration-driven Spring AI + Google ADK flow compilation, in-memory sessions, synchronous events, and streaming execution | [Agent Flow Starter](egon-cola-component-agent-flow-starter/README.md) |
| Common | Shared contracts and utility modules | [Common](egon-cola-component-common/README.md) |
| Rule Engine | Java rule chains, trees, and execution control | [Rule Engine](egon-cola-component-rule-engine-starter/README.md) |
| Dynamic Thread Pool | Runtime executor management | [Dynamic Thread Pool](egon-cola-component-dynamic-thread-pool/README.md) |
| RPC | Protobuf/gRPC and Tianshu integration | [RPC](egon-cola-component-rpc/README.md) |

The Agent Flow starter is default-off and follows the flat component profile. It owns configuration, ADK graph compilation, registry/session execution, cancellation, timeout, and bounded close. The host owns the provider `ChatModel`, credentials, tools, MCP, HTTP, authentication, and database or other persistence boundaries.

## BOM

Import [`egon-cola-components-bom`](egon-cola-components-bom/README.md) to consume `top.egon:egon-cola-component-agent-flow-starter` and other published component entry points without repeating versions. The BOM exports Egon runtime artifacts only; provider libraries, ADK internals, admin applications, test modules, and aggregator POMs remain outside its public dependency list.

## Architecture

Read the [Components architecture guide](egon-cola-components-architecture.md) before adding a module. The guide distinguishes the ordinary starter/admin/test layout from the approved flat Agent Flow starter and records its Java 21, Spring Boot 3.5.16, Spring AI 1.1.8, and Google ADK 0.7.0 compatibility line.

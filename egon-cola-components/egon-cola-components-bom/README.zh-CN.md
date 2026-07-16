# egon-cola-components-bom

[English](README.md) | 中文

## 简要介绍

`egon-cola-components-bom` 是 Egon COLA 组件体系的 Maven BOM。它不提供运行时代码，只负责统一管理 `egon-cola-components` 下可被业务应用直接依赖的组件版本，避免业务工程在每个组件依赖上重复写版本号。

BOM 当前导出的是稳定消费入口：common 的具体子模块、各业务组件的 starter，以及字节码组件的公开 API、桥接层、运行时、Agent 和 starter。admin、test、聚合 POM 不作为业务依赖入口导出。

## 功能说明

### 统一版本管理

业务应用通过 `dependencyManagement` import BOM 后，后续声明组件依赖时不需要再写 `<version>`。所有组件版本跟随 BOM 的 `project.version`，当前为 `5.2.3`。

### 导出的依赖清单

| Artifact | 用途 |
|---|---|
| `egon-cola-component-common-core` | 错误状态、异常、枚举契约 |
| `egon-cola-component-common-model` | 请求、查询、分页模型 |
| `egon-cola-component-common-trace` | MDC `traceId` 上下文 |
| `egon-cola-component-common-result` | 对外响应 DTO 和内部结果 Model |
| `egon-cola-component-common-id` | UUIDv7 和 ID 生成 |
| `egon-cola-component-common-crypto` | 摘要、HMAC、Base64、Hex |
| `egon-cola-component-common-mask` | 数据脱敏 |
| `egon-cola-component-common-structure` | 树结构构建 |
| `egon-cola-component-dynamic-thread-pool-starter` | 动态线程池业务侧 starter |
| `egon-cola-component-dynamic-config-center-starter` | 动态配置中心业务侧 starter |
| `egon-cola-component-rule-engine-starter` | 规则引擎 starter |
| `egon-cola-component-access-guard-starter` | 方法访问治理 starter |
| `egon-cola-component-method-extension-starter` | 方法扩展 starter |
| `egon-cola-component-bytecode-api` | 字节码能力公共 API |
| `egon-cola-component-bytecode-bridge` | 业务应用与 Agent 之间的桥接层 |
| `egon-cola-component-bytecode-runtime` | 字节码运行时实现 |
| `egon-cola-component-bytecode-agent` | Java Agent 入口 |
| `egon-cola-component-bytecode-starter` | 字节码能力 Spring Boot starter |

### 不导出的模块

| Module | 不导出原因 |
|---|---|
| `egon-cola-component-common` | 聚合 POM，不是运行时 Jar |
| `*-admin` | 独立服务，应按应用部署，不作为业务依赖 |
| `*-test` | 组件样例和验证模块，不应进入业务运行时 |
| `egon-cola-component-dynamic-thread-pool` / `dynamic-config-center` / `rule-engine` / `access-guard` / `method-extension` / `bytecode` | 组件聚合 POM，不是业务依赖入口 |

## 完整的使用示例

### 1. 在业务工程中导入 BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 按需引入组件

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-result</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-model</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard-starter</artifactId>
    </dependency>
</dependencies>
```

### 3. 在多模块业务项目中集中声明版本

```xml
<properties>
    <egon-cola.version>5.2.3</egon-cola.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

子模块只声明 artifact：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
</dependency>
```

## 设计思想和实现细节

### 设计思想

1. BOM 只管理消费者真正需要的运行时入口，避免 admin/test/聚合模块被业务误依赖。
2. common 采用细粒度导出，业务按能力选择，避免一个 common 大包传递过多依赖。
3. 常规业务组件只导出 starter，保持 Spring Boot 自动配置入口明确；字节码组件按公开的 API、桥接、运行时、Agent 和 starter 边界分别管理版本。
4. 版本统一跟随 BOM 自身版本，降低组件组合使用时的版本漂移风险。

### 实现细节

- `packaging` 为 `pom`，没有源码和运行时类。
- 所有导出组件都在 `<dependencyManagement>` 中声明，版本使用 `${project.version}`。
- release profile 负责源码包、javadoc、GPG 签名和 Central Portal 发布配置。
- `maven-deploy-plugin` 默认 `skip=true`，发布路径由 Central Publishing profile 控制。

## 边界和注意事项

- 业务应用不能只依赖 BOM；BOM 只能放在 `dependencyManagement` 中 import。
- admin 模块需要按独立 Spring Boot 应用构建部署，不通过 BOM 作为业务依赖使用。
- 新增组件时，应优先导出 starter，而不是导出组件聚合 POM 或 test 模块；只有存在明确、独立的消费边界时才导出额外模块。
- 若 common 新增子模块，需要明确它是否是业务运行时稳定入口，再决定是否加入 BOM。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-components-bom -am test
```

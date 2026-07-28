# Gateway Operation Schema Presentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 展开 Gateway Operation 的 HTTP/RPC 请求与响应 Schema，并清楚展示每个字段的类型、必填、说明和约束。

**Architecture:** Gateway Starter 在 Definition 上报时把 Protobuf Descriptor 转成有界递归 Schema，并从 `GatewayOperation` 合并字段说明。Gateway Admin Web 将统一 Schema 转成 Composite 风格的树行，由 Ant Design Table 默认展开，同时保留折叠的原始 JSON。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Protobuf 4.32、React 19、TypeScript 6、Ant Design 6、Vitest、JUnit 5、Maven。

## Global Constraints

- 当前 `main` 分支 inline 执行，不创建子代理或 worktree。
- 不增加依赖，不修改数据库迁移，不重启或新启动服务。
- 保留两个既有未跟踪文件，不纳入任何提交。
- 每个生产行为必须先有会失败的测试。
- 未提供字段说明时显示“暂无字段说明”，不得生成虚假业务注释。

---

### Task 1: Starter RPC Schema 与字段说明契约

**Files:**
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/annotation/GatewaySchemaField.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/ProtobufSchemaMapper.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/GatewaySchemaDescriptions.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/annotation/GatewayOperation.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/RpcGatewayDefinitionContributor.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/GatewayHttpOperationMapper.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/src/test/java/top/egon/cola/component/gateway/starter/discovery/ProtobufSchemaMapperTest.java`

**Interfaces:**
- Consumes: `Descriptors.Descriptor` 和 `GatewaySchemaField[]`。
- Produces: `ProtobufSchemaMapper.schema(Descriptors.Descriptor, GatewaySchemaField[])` 返回 `Map<String, Object>`；`GatewaySchemaDescriptions.apply(Map<String,Object>, GatewaySchemaField[])` 为 HTTP Schema 合并说明。

- [ ] **Step 1: 写失败测试**

用手工 `DescriptorProtos.FileDescriptorProto` 构造 `CreateOrderRequest`、嵌套
`Address`、重复 `sku` 和 enum 字段；通过 `Descriptors.FileDescriptor.buildFrom` 得到真实
Descriptor。断言字面量：`customerId.type=string`、`sku.type=array`、
`deliveryAddress.properties.province.description=省份`、enum 值完整；另一个测试断言未知
说明路径抛出异常。

- [ ] **Step 2: 验证 RED**

Run:

```bash
./mvnw -B -ntp -pl :egon-cola-component-gateway-starter -am \
  -Dtest=ProtobufSchemaMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 测试编译失败，因为 `GatewaySchemaField` 和 `ProtobufSchemaMapper` 尚不存在。

- [ ] **Step 3: 最小实现**

实现注解：

```java
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewaySchemaField {
    String path();
    String description();
}
```

在 `GatewayOperation` 增加：

```java
GatewaySchemaField[] requestSchemaFields() default {};
GatewaySchemaField[] responseSchemaFields() default {};
```

`ProtobufSchemaMapper` 递归映射标量、enum、message、repeated、map，并在完成后校验所有
说明路径均被消费。`RpcGatewayDefinitionContributor` 用真实 method input/output Descriptor
代替原先仅含 `type/messageType` 的 Map。HTTP Mapper 在生成 request/response Schema 后
调用 `GatewaySchemaDescriptions.apply`。

- [ ] **Step 4: 验证 GREEN**

重复 Step 2 命令，Expected: `ProtobufSchemaMapperTest` PASS。

- [ ] **Step 5: 提交**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter
git commit -m "feat: expand gateway operation schemas"
```

### Task 2: 测试 Provider 字段说明示例

**Files:**
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-contract/src/main/java/top/egon/cola/component/gateway/test/rpc/contract/OrderRpc.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-contract/src/main/java/top/egon/cola/component/gateway/test/rpc/contract/EchoRpc.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-http-provider/src/main/java/top/egon/cola/component/gateway/test/http/OrderController.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-contract/src/test/java/top/egon/cola/component/gateway/test/rpc/contract/GatewayRpcContractTest.java`

**Interfaces:**
- Consumes: Task 1 的 `GatewaySchemaField` 注解。
- Produces: 本机测试 Provider 重新上报后可见的中文字段说明。

- [ ] **Step 1: 写失败测试**

扩展 `GatewayRpcContractTest`，读取 `OrderRpc.createOrder` 的 `GatewayOperation`，断言
request 字段说明包含 `customerId=客户编号`、`sku=商品 SKU 列表`、
`deliveryAddress.province=配送省份`，response 字段包含 `orderId=订单编号`。

- [ ] **Step 2: 验证 RED**

Run:

```bash
./mvnw -B -ntp -pl :egon-cola-component-gateway-test-rpc-contract -am \
  -Dtest=GatewayRpcContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 字段说明断言失败，因为测试 Contract 尚未声明这些元数据。

- [ ] **Step 3: 最小实现**

在 RPC Contract 和 HTTP `OrderController` 的 `GatewayOperation` 中补齐请求、响应字段
说明；每个 path 使用 JSON 字段名，嵌套字段使用点路径。

- [ ] **Step 4: 验证 GREEN 并提交**

重复 Step 2 命令后提交：

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test
git commit -m "test: document gateway provider schemas"
```

### Task 3: Admin Web 递归 Schema 表格

**Files:**
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/interface-catalog/schemaRows.ts`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/interface-catalog/schemaRows.test.ts`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/interface-catalog/SchemaPanel.tsx`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/interface-catalog/SchemaPanel.test.tsx`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/interface-catalog/OperationPage.tsx`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/styles/index.css`

**Interfaces:**
- Consumes: `Record<string, unknown>` 的 request/response Schema。
- Produces: `schemaRows(schema): SchemaRow[]` 和 `<SchemaPanel title schema />`。

- [ ] **Step 1: 写转换失败测试**

用字面量 fixture 覆盖 object、required、nested object、array、enum、description、format
和 protobufType。断言完整路径、`array<string>` 类型标签、必填与约束字符串。

- [ ] **Step 2: 验证 RED**

Run:

```bash
npm test -- --run src/features/interface-catalog/schemaRows.test.ts
```

Expected: 模块不存在而失败。

- [ ] **Step 3: 实现纯转换函数并验证 GREEN**

实现 `SchemaRow`：

```ts
export type SchemaRow = {
  key: string
  name: string
  path: string
  type: string
  required: boolean
  description?: string
  technicalType?: string
  constraints: string[]
  children?: SchemaRow[]
}
```

递归读取 `properties/items/additionalProperties`，从父节点 `required` 计算必填。重复 Step 2
命令，Expected: PASS。

- [ ] **Step 4: 写组件失败测试**

渲染带嵌套字段的 `SchemaPanel`，断言“字段/类型/必填/说明/约束”列、父子字段和
“暂无字段说明”均可见，且原始 Schema 位于折叠区。

- [ ] **Step 5: 验证 RED、实现并验证 GREEN**

先运行：

```bash
npm test -- --run src/features/interface-catalog/SchemaPanel.test.tsx
```

Expected: `SchemaPanel` 不存在。随后使用 Ant Design Table，设置
`expandable={{ defaultExpandAllRows: true }}`，在 `OperationPage` 替换 Request/Response
的 `JsonPanel`。重复命令，Expected: PASS。

- [ ] **Step 6: 提交**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web
git commit -m "feat: render expanded operation schemas"
```

### Task 4: 全量验证

**Files:**
- Verify only; no production file is owned by this task.

**Interfaces:**
- Consumes: Tasks 1-3 的提交。
- Produces: 可交付的测试、构建和工作区证据。

- [ ] **Step 1: Maven 验证**

```bash
./mvnw -B -ntp -pl :egon-cola-component-gateway-starter,:egon-cola-component-gateway-test-rpc-contract -am test
```

- [ ] **Step 2: 前端验证**

```bash
npm test -- --run
npm run typecheck
npm run lint
npm run build
```

- [ ] **Step 3: 工作区与页面资源验证**

```bash
git diff --check
git status --short
curl -fsS http://127.0.0.1:5173/operations/019fa6db30137bd290f413353162664e >/dev/null
```

只允许保留两个用户已有未跟踪文件。已运行的 Vite 可热加载前端代码；Provider 端新增
Schema 必须由用户后续重启/重新上报后才会形成新 Definition。

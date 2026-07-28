# Gateway 接口 Schema 展示优化设计

## 背景

Gateway Admin 的 Operation 详情目前直接用 JSON 面板展示 `requestSchema`、
`responseSchema`。HTTP 上报至少包含 `properties`，但 RPC 上报只有
`type=protobuf` 和 `messageType`；因此 RPC 请求/响应无法看到字段、嵌套对象、数组、
枚举、Protobuf 原始类型和字段说明。

## 目标

- HTTP 与 RPC 请求/响应 Schema 使用同一棵递归字段树展示。
- 对象、数组、Map、枚举和嵌套消息默认展开。
- 每行展示字段路径、JSON 类型、协议/Java 类型、必填状态、说明和约束。
- RPC Starter 从现有 Protobuf Descriptor 生成字段 Schema，不让浏览器解析
  `base64DescriptorSet`。
- Provider 可以通过 Gateway 自有注解声明请求/响应字段说明；没有说明时页面明确显示
  “暂无字段说明”，不根据字段名伪造业务语义。

## 方案比较

### 方案一：Starter 展开 Schema，Admin Web 渲染递归树表（采用）

在 `gateway-starter` 内把 Protobuf Descriptor 转为受限 JSON Schema 形态，并让
HTTP/RPC 共用字段说明元数据。Admin Web 只消费统一 Schema。

优点：Schema 在上报边界完成校验并持久化，页面轻量，历史 Definition 可复现，不增加
前端 Protobuf 依赖。缺点：需要 Provider 重新上报 Definition 才能看到新字段。

### 方案二：Admin Web 解析 base64 Descriptor

优点是后端改动较少；缺点是需要新增 Protobuf JS 依赖、把大 Descriptor 送进浏览器，
字段说明仍没有稳定来源，且历史页面逻辑与 Engine 的 Descriptor 解析容易漂移，因此不采用。

### 方案三：只增强原始 JSON Viewer

只能改善折叠和配色，无法补齐 RPC 字段，也不能形成清晰的类型/说明列，因此不采用。

## 后端设计

新增 `GatewaySchemaField` 注解，并在 `GatewayOperation` 增加带默认值的
`requestSchemaFields`、`responseSchemaFields`。这是对现有注解元数据模式的兼容扩展，
旧 Provider 无需修改即可运行。

`ProtobufSchemaMapper` 负责把请求/响应 `Descriptors.Descriptor` 转成：

```text
type = object
messageType = Protobuf 消息全名
properties
  fieldName
    type = string | integer | number | boolean | object | array
    format
    protobufType
    protobufName
    fieldNumber
    description
    enum / items / properties
```

重复字段映射为数组，消息字段递归展开，Map 映射为 object + additionalProperties，循环
引用和深度上限使用 `$ref`/`truncated`，避免无界 Schema。字段说明使用注解中以 JSON
字段路径声明的内容，例如 `deliveryAddress.province`。未知路径视为配置错误，避免注释拼写
错误被静默忽略。

HTTP 继续使用现有 Jackson Schema 生成器，但同样应用字段说明注解，保证两种协议的
字段说明入口一致。

## 前端设计

新增纯函数 `schemaRows`，把 JSON Schema 转成带 `children` 的行模型；新增
`SchemaPanel`，用 Ant Design Table 默认展开所有层级。列为：

- 字段：当前字段名和完整路径；
- 类型：JSON 类型，并补充 format、`javaType`、`protobufType`、`messageType`；
- 必填：来自父节点 `required` 或字段自身元数据；
- 说明：Schema `description`，缺失时显示“暂无字段说明”；
- 约束：enum、default、范围、长度、pattern、oneOf/anyOf/allOf 等已有信息。

原始 Schema 放进折叠区保留，便于排查上报数据，不再作为主视图。

## 兼容性与边界

- 不新增数据库迁移；Schema 仍存入现有 JSONB 字段。
- 不改变 Rule Snapshot 的运行时请求/响应字符串契约。
- 不修改现有 Definition；Provider 重新启动或触发上报后产生新 Definition 版本。
- 不引入新前端或后端依赖。
- Error Schema 暂时保留原始 JSON 展示，本次只处理用户要求的 Request/Response。

## 测试

- Starter：以真实 Protobuf Descriptor 验证嵌套消息、重复字段、标量格式、枚举、字段
  说明和未知说明路径。
- Admin Web：验证递归行转换、数组/对象类型、必填、说明、约束以及默认展开渲染。
- 运行 Gateway Starter Maven 测试、前端 Vitest、TypeScript、ESLint 与生产构建。

## 设计模式判断

Schema 本身天然是递归组合结构，前端行模型使用 Composite 风格的 `children`，但不引入
额外类层次。后端保持现有 Mapper 模式；Strategy、Factory 或访问者模式不会减少当前
分支复杂度，反而会扩大接口面，因此不采用。

# DDC Admin Web 下拉化改造设计（2026-07-31）

## 背景与目标

DDC admin-web 的筛选器和新建表单中，`appCode` / `env` / `namespace` 目前都是
自由文本输入框，需要人工记忆并键入，容易输错且与后端已有数据脱节。本次改造把
这些字段改为**可选下拉框**（可搜索、支持直接输入新值兜底），`configKey` 因业务
数据量大保持自由输入。

## 业务语义（用户确认）

- **env**：全局固定环境枚举（dev / test / sit / gray / prod），与 appCode 无关。
- **namespace**：业务域隔离维度；**一个业务域下可以有多个 appCode**。
- **appCode**：应用，归属某个业务域。
- 数据模型**不改**：配置标识仍为 (appCode, env, namespace) 三元组；
  "业务域 → 应用"关系从 `ddc_namespace` 现有数据反向推导
  （该域在数据中出现过即视为域内应用）。

## 决策

| 决策点 | 结论 |
|---|---|
| env 选项来源 | **前端内置固定枚举** dev/test/sit/gray/prod + 可输入新值兜底，不依赖任何数据 |
| namespace（业务域）选项 | 新增后端端点：全量去重 namespace 列表 |
| 业务域 → 应用 推导 | `GET /api/v1/ddc/apps` 增加可选 `namespace` 参数：返回在 `ddc_namespace` 中归属该域的应用 |
| 级联顺序 | **namespace（业务域）→ appCode（域内应用）→ env（环境）**；env 与 namespace 相互独立，互不级联 |
| 空库/无选项兜底 | 下拉 + 可输入新值（antd `Select mode="tags" maxCount={1} showSearch`），保存沿用 `ensureAppAndNamespace` 自动创建 |
| 改造范围 | 筛选栏（6 处）+ 表单对话框（3 处）全改；configKey 保持自由输入 |
| 实现方式 | 共享组件族（`AppSelect` / `EnvSelect` / `NamespaceSelect` / `ScopeSelects`）+ `useScopeOptions` 会话级缓存 hook |
| 编辑态 | 配置编辑对话框的 app/env/namespace 保持锁定（现状不变） |
| 新建应用对话框 | 不变（appCode/appName 本来就是新值） |
| 选项缓存 | 会话级内存 Map（key=请求签名），不持久化到 localStorage |

## 后端端点

`DdcNamespaceController` 新增：

```java
@GetMapping("/domains")
public ResultRecord<List<String>> domains()
```

- 返回全量去重 namespace 值（业务域），按值升序；无数据返回空列表。
- Repository 查询：`SELECT DISTINCT n.namespace FROM DdcNamespaceEntity n ORDER BY n.namespace`。

`DdcAppController.list` 增加可选参数：

```java
@GetMapping
public ResultRecord<List<DdcAppEntity>> list(
        @RequestParam(value = "namespace", required = false) String namespace)
```

- `namespace` 为空：返回全部应用（现状不变）。
- `namespace` 非空：返回在 `ddc_namespace` 中存在该域行的应用
  （Repository：`SELECT DISTINCT n.appCode FROM DdcNamespaceEntity n WHERE n.namespace = :namespace`，
  再按 appCode 查应用列表）。
- 未知域返回空列表（不报错）。

原方案中的 `GET /namespaces/envs?appCode=` **取消**（env 为固定枚举，不再需要）。

## 组件与数据流

```
NamespaceSelect（业务域，可输入下拉）
  └─ options ← GET /api/v1/ddc/namespaces/domains     （新增端点，全局去重）
AppSelect（应用，可输入下拉）
  └─ options ← GET /api/v1/ddc/apps?namespace=xxx     （域内应用；未选域时全部应用）
EnvSelect（环境，可输入下拉）
  └─ options ← 前端常量 ['dev','test','sit','gray','prod']，无后端请求
useScopeOptions：内存 Map 缓存；域/应用列表随上级变化自动重载并失效旧缓存；
env 为常量无需加载
```

- 级联：**namespace 变化 → 清空并重载 appCode**；env 独立不级联。
- 上级未选时下级 Select **不禁用**，placeholder 显示"请先选择业务域"等提示；
  手输兜底始终可用（输入任意值保存时由 `ensureAppAndNamespace` 兜底创建）——
  级联只负责自动加载选项，不强制约束输入。
- 选项加载失败：`message.error` 提示，下拉退化为可输入框，不阻塞手输。

## 页面接入清单

**筛选栏（6 处）：**

| 页面 | 改动 |
|---|---|
| 服务注册 | env/namespace 手输框 → `EnvSelect` + `NamespaceSelect`（无 appCode） |
| 配置管理 | namespace/appCode/env → `ScopeSelects`（域→应用→环境）；configKey 保持手输 |
| 命名空间 | appCode/env → `AppSelect` + `EnvSelect`（沿用现有列表页语义） |
| 实例 | namespace/appCode/env → `ScopeSelects` |
| 缓存 | namespace/appCode/env → `ScopeSelects` |

**表单对话框（3 处）：**

| 对话框 | 改动 |
|---|---|
| 新建配置 | namespace → `NamespaceSelect`（业务域，可输入新域），appCode → `AppSelect`（域内过滤），env → `EnvSelect`；configKey 保持输入；valueType 不变 |
| 新建命名空间 | appCode → `AppSelect`，env → `EnvSelect`，namespace 保持输入（创建新业务域=新值） |
| 新建应用 | 不变 |

## 交互细节

- 下拉搜索：`showSearch` + 按值过滤；appCode 选项展示 `appCode（appName）`。
- 空选项提示："无数据，可直接输入新值"。
- 输入新值：tags 模式下直接输入任意文本即为值；保存时 `ensureAppAndNamespace`
  自动创建应用/命名空间（沿用现状逻辑，含 message 提示"作用域已就绪"）。
- env 选项为常量枚举，也允许直接输入自定义环境值。

## 错误处理

| 场景 | 行为 |
|---|---|
| 选项加载失败 | `message.error` 提示；下拉退化为可输入框 |
| 上级未选 | 下级 Select 不禁用，placeholder 提示；手输兜底可用 |
| 后端 401 | 沿用全局处理：清 token 回登录页 |
| 空库 | 下拉无选项 + 手输兜底；保存自动创建 |

## 测试

- 后端：`domains` 端点——去重、排序、无数据返回空列表；`apps?namespace=`——
  过滤正确、未知域空列表、无参时返回全部（现状回归）。
- 前端：`useScopeOptions` 缓存与级联失效单测；`ScopeSelects` 渲染、域→应用级联
  清空、env 常量选项、可输入新值组件测试；现有页面测试更新 mock 后回归全绿。

## 验收标准

- 6 处筛选栏 + 新建配置/新建命名空间对话框的 app/env/namespace 全部为可选下拉，
  支持搜索与输入新值；env 下拉为固定枚举。
- 级联行为正确：业务域变化时应用列表清空并重载（按域过滤）；env 独立不级联。
- 空库场景可完成全流程：直接输入新值 → 保存 → 自动创建应用/命名空间。
- 前端 vitest/typecheck/lint 全绿；后端 mvn test 全绿。

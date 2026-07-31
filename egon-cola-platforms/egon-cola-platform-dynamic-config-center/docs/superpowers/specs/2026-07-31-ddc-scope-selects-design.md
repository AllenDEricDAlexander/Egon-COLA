# DDC Admin Web 下拉化改造设计（2026-07-31）

## 背景与目标

DDC admin-web 的筛选器和新建表单中，`appCode` / `env` / `namespace` 目前都是
自由文本输入框，需要人工记忆并键入，容易输错且与后端已有数据脱节。本次改造把
这些字段改为**可选下拉框**（可搜索、支持直接输入新值兜底），`configKey` 因业务
数据量大保持自由输入。

## 决策

| 决策点 | 结论 |
|---|---|
| env 选项来源 | 后端新增 `GET /api/v1/ddc/namespaces/envs?appCode=`（去重排序） |
| 空库/无选项兜底 | 下拉 + 可输入新值（antd `Select mode="tags" maxCount={1} showSearch`），保存沿用 `ensureAppAndNamespace` 自动创建 |
| 改造范围 | 筛选栏（6 处）+ 表单对话框（3 处）全改；configKey 保持自由输入 |
| 实现方式 | 共享组件族（`AppSelect` / `EnvSelect` / `NamespaceSelect` / `ScopeSelects`）+ `useScopeOptions` 会话级缓存 hook |
| 编辑态 | 配置编辑对话框的 app/env/namespace 保持锁定（现状不变） |
| 新建应用对话框 | 不变（appCode/appName 本来就是新值） |
| 选项缓存 | 会话级内存 Map（key=请求签名），不持久化到 localStorage |

## 组件与数据流

```
AppSelect（可输入下拉）
  └─ options ← GET /api/v1/ddc/apps      （展示 appCode + appName）
EnvSelect（可输入下拉）
  └─ options ← GET /api/v1/ddc/namespaces/envs?appCode=xxx   （新增端点）
NamespaceSelect（可输入下拉）
  └─ options ← GET /api/v1/ddc/namespaces?appCode=xxx&env=xxx（现有端点）
useScopeOptions：内存 Map 缓存；AppSelect 数据会话级缓存，
env/namespace 随上级变化自动重载并失效旧缓存
```

- 级联：app 变化 → 清空并重载 env/namespace；env 变化 → 清空并重载 namespace。
- 上级未选时下级 Select **不禁用**，placeholder 显示"请先选择 app"等提示；
  手输兜底始终可用（输入任意值保存时由 `ensureAppAndNamespace` 兜底创建）——
  级联只负责自动加载选项，不强制约束输入。
- 选项加载失败：`message.error` 提示，下拉退化为可输入框，不阻塞手输。

## 后端端点

`DdcNamespaceController` 新增：

```java
@GetMapping("/envs")
public ResultRecord<List<String>> envs(@RequestParam("appCode") String appCode)
```

`DdcNamespaceRepository`（或 Service）新增查询：
`SELECT DISTINCT n.env FROM DdcNamespaceEntity n WHERE n.appCode = :appCode ORDER BY n.env`
- 去重、按 env 升序；无数据返回空列表；未知 appCode 返回空列表（不报错）。

## 页面接入清单

**筛选栏（6 处）：**

| 页面 | 改动 |
|---|---|
| 服务注册 | env/namespace 手输框 → `ScopeSelects`（env、namespace 两项，无 appCode） |
| 配置管理 | appCode/env/namespace → `ScopeSelects`；configKey 保持手输 |
| 命名空间 | appCode/env → `AppSelect` + `EnvSelect` |
| 实例 | appCode/env/namespace → `ScopeSelects` |
| 缓存 | appCode/env/namespace → `ScopeSelects` |

**表单对话框（3 处）：**

| 对话框 | 改动 |
|---|---|
| 新建配置 | app/env/namespace → `ScopeSelects`；configKey 保持输入；valueType 不变 |
| 新建命名空间 | appCode → `AppSelect`，env → `EnvSelect`，namespace 保持输入（新值） |
| 新建应用 | 不变 |

## 交互细节

- 下拉搜索：`showSearch` + 按值过滤（appCode 匹配），选项展示 `appCode（appName）`。
- 空选项提示："无数据，可直接输入新值"。
- 输入新值：tags 模式下直接输入任意文本即为值；保存时 `ensureAppAndNamespace`
  自动创建应用/命名空间（沿用现状逻辑，含 message 提示"作用域已就绪"）。

## 错误处理

| 场景 | 行为 |
|---|---|
| 选项加载失败 | `message.error` 提示；下拉退化为可输入框 |
| 上级未选 | 下级 Select 不禁用，placeholder 提示；手输兜底可用 |
| 后端 401 | 沿用全局处理：清 token 回登录页 |
| 空库 | 下拉无选项 + 手输兜底；保存自动创建 |

## 测试

- 后端：`envs` 端点——去重、排序、按 appCode 过滤、无数据/未知 app 返回空列表。
- 前端：`useScopeOptions` 缓存与级联失效单测；`ScopeSelects` 渲染、级联清空、
  可输入新值组件测试；现有页面测试更新 mock 后回归全绿。

## 验收标准

- 6 处筛选栏 + 新建配置/新建命名空间对话框的 app/env/namespace 全部为可选下拉，
  支持搜索与输入新值。
- 级联行为正确：上级变化时下级清空并重载。
- 空库场景可完成全流程：直接输入新值 → 保存 → 自动创建应用/命名空间。
- 前端 vitest/typecheck/lint 全绿；后端 mvn test 全绿。

# DDC 作用域模型修订设计（2026-07-31）

## 背景与目标

当前模型与用户期望不符：注册身份无 app/biz 维度（无法实现应用级禁用门控）；
命名空间依赖环境（新建要选 env）；环境为前端写死枚举（无法管理 dev/test/prod
并存与流量隔离）；业务域此前是数据推导无管理面。本次修订确立完整层级模型并
实现管理面、注册契约、禁用门控。

## 目标模型

```
biz（业务域，独立实体，app 必填归属）
 └─ app（应用，属于 biz）
     └─ ns（命名空间，属于 app，(app, ns) 唯一，不依赖 env）
env（环境，独立实体）—— 服务发现与配置的隔离维度，与 biz/app/ns 正交
注册身份恒为：biz-ns-env-app
```

- 配置标识不变：(app, env, ns, key)。
- env 必须隔离（dev 开发 / test 测试 / prod 生产并存），流量不能跨环境。
- 禁用语义：biz/app/ns/env 任一 `enabled=false` → 该作用域新注册被拒、配置拉取被拒。

## 决策

| 决策点 | 结论 |
|---|---|
| 注册身份 | `DdcServiceKey` 增加 `bizCode` + `appCode`（恒有值），Redis key 结构随之变化；SDK 消费者零改动（appCode 取自现有 `DdcProperties.appCode`，bizCode 新增配置 `egon.cola.component.ddc.biz-code`，缺失时注册被拒并提示） |
| biz 建模 | 独立实体 `ddc_biz`（biz_code 唯一 + 名称 + 描述 + enabled），独立管理页；app 必填归属 |
| env 建模 | 独立实体 `ddc_env`（env_code 唯一 + 描述 + sort_order + enabled），独立管理页；前端删除写死的 `ENV_OPTIONS` 常量，环境下拉从后端拉取 |
| ns 建模 | `ddc_namespace` 去掉 env 列，(app, ns) 唯一；新建命名空间不选环境；V5 迁移去重 |
| 删除语义 | 保护性删除：biz 下有 app、app 下有 ns、ns 下有配置、env 被引用 → 拒绝（`DDC_*_IN_USE`）；无子数据才可删；禁用不删数据 |
| 禁用门控 | 注册/拉取路径校验四实体 enabled（短缓存 5s），任一禁用 → `DDC_SCOPE_DISABLED`；心跳/续租不逐次校验，存量 lease 自然过期收敛 |
| 服务注册页 | 重构为 APP 维度：主列表 = 有实例的 APP；点击 APP 弹 Drawer 按服务分组看实例；**实例页删除** |
| 菜单 | 业务域、环境、服务注册、配置管理、应用、命名空间、发布任务、缓存（8 项） |
| 旧端点 | `GET /namespaces/domains` 删除（业务域改为独立实体） |

## 数据模型与迁移（V5）

```
ddc_biz       新增：biz_code(唯一) biz_name description enabled created_at updated_at
ddc_env       新增：env_code(唯一) description sort_order enabled created_at updated_at
ddc_app       修改：+ biz_code（必填，外键 ddc_biz）
ddc_namespace 修改：- env 列；唯一约束 (app_code, namespace)
```

- 迁移：`ddc_namespace` 按 (app_code, namespace) 去重（保留最早行）后删 env 列重建唯一约束；
  插入默认业务域 `default`（enabled=true）并回填 `ddc_app.biz_code`。
- 配置表不动。

## 后端 API

**四实体管理（统一模式）**：

| 实体 | 端点 | 能力 |
|---|---|---|
| biz | `GET/POST /api/v1/ddc/bizs`，`PUT/DELETE /api/v1/ddc/bizs/{code}` | 列表（名称模糊）、新建、编辑、删除（保护性）、启用/禁用 |
| env | `GET/POST /api/v1/ddc/envs`，`PUT/DELETE /api/v1/ddc/envs/{code}` | 同上 + 排序 |
| app | `GET/POST /api/v1/ddc/apps`，`PUT/DELETE /api/v1/ddc/apps/{code}` | 列表（biz 过滤 + 名称模糊）、新建（必选 biz）、编辑、删除、禁用 |
| ns | `GET/POST /api/v1/ddc/namespaces`，`PUT/DELETE /api/v1/ddc/namespaces/{id}` | 列表（app 过滤 + ns 模糊，无 env）、新建（app + ns 名）、删除、禁用 |

- 删除保护错误码：`DDC_BIZ_IN_USE` / `DDC_APP_IN_USE` / `DDC_NAMESPACE_IN_USE` / `DDC_ENV_IN_USE`。
- 现有 app 端点 `GET /apps?namespace=`（域内应用推导）删除——应用筛选改为 `?biz=`。

**注册契约（starter）**：
- `DdcServiceKey` 增加 `bizCode`、`appCode`（构造时必填）；
- `DdcKeys` 注册 key 含 biz/app；`DdcServiceRegistration`、`DdcServiceLeaseRequest`、
  `DdcServiceQuery`、`DdcServiceSnapshot`、`DdcRegistryEvent` 相应透传；
- Lua 脚本（register/heartbeat/deregister/expire）key 与参数随结构调整；
- 注册/拉取校验：查四实体 enabled（批量 + 5s 短缓存）→ 禁用返回 `DDC_SCOPE_DISABLED`。
- registry 查询响应（`/registry/services`、`/registry/instances`）带 `appCode` 字段，
  前端按 appCode 聚合为 APP 列表。

## 前端页面

**四个管理页**：

| 页面 | 列表 | 筛选 | 操作 |
|---|---|---|---|
| 业务域（新增） | biz_code / 名称 / 描述 / 状态 | 名称模糊 | 新建、编辑、禁用、删除（保护性） |
| 环境（新增） | env_code / 描述 / 排序 / 状态 | 名称模糊 | 新建、编辑、禁用、删除 |
| 应用（改造） | biz / appCode / 名称 / owner / 状态 | biz 下拉 + appCode/名称模糊 | 新建（必选 biz）、编辑、禁用、删除 |
| 命名空间（改造） | app / ns / 描述 / 状态 | app 下拉 + ns 模糊 | 新建（app + ns 名，无 env）、禁用、删除 |

**服务注册页（重构，替代实例页）**：

```
筛选：biz → app → ns → env 四层级联
主视图：APP 列表（有实例的 APP：appCode、业务域、环境、ns、在线实例数）
  └─ 点击 APP 行 → Drawer 抽屉：按服务分组（kind/name/group/version → 实例表格）
      实例列：状态徽标 / instanceId / host:port / 最近心跳 / 过期时间
      抽屉头部：app 信息 + 刷新 + 分组计数
概览卡：APP 数、在线实例数、HTTP/RPC/Gateway 服务数
```

**配置管理**：biz → app → ns → env 级联 + configKey；新建对话框同级联。
**缓存页**：biz → app → ns → env 级联。
**实例页删除**；菜单 8 项。

**前端数据源**：
- 业务域 ← `GET /bizs`；应用 ← `GET /apps?biz=`；命名空间 ← `GET /namespaces?appCode=`（无 env）；环境 ← `GET /envs`。
- `useScopeOptions` 级联链改 biz → app → ns，env 独立从后端拉；删除 `ENV_OPTIONS` 常量与 domains 逻辑。
- 空库兜底保留：可输入新值；自动创建 app 归属当前选中 biz、自动创建 ns 挂 app 下；biz 不自动创建（先去业务域页建）。

## 测试

- 后端：V5 迁移（去重/回填/约束）、四实体 CRUD 与删除保护错误码、注册契约（key 结构/Lua/序列化）、
  门控（register/pull 被禁返回 `DDC_SCOPE_DISABLED`、启用恢复、心跳不校验）、现有测试回归更新。
- 前端：`useScopeOptions` 级联改造、四管理页组件测试、服务注册页聚合与抽屉测试、
  配置/缓存页筛选回归、全量 typecheck/lint/vitest 全绿。

## 验收标准

- 注册身份恒为 biz-ns-env-app；任一实体禁用 → 新注册被拒、配置拉取被拒。
- biz/app/ns/env 四页可完整管理（增删改查 + 筛选 + 禁用），删除有保护。
- 新建命名空间不选环境；配置管理按 (app, env, ns) 正常建配置。
- 服务注册页按 APP 聚合展示、抽屉看实例；实例页已移除。
- 环境下拉来自后端实体；前端无写死环境枚举。
- 前后端测试全绿。

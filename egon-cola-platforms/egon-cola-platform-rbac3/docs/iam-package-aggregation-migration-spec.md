# RBAC3 Admin IAM 包聚合迁移 Spec

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 适用模块 | `egon-cola-platform-rbac3-admin` |
| 变更类型 | Java package 聚合迁移 |
| 目标根包 | `top.egon.cola.platform.rbac3.admin.iam` |
| 当前基线 | `main` 分支当前 RBAC3 Admin 实现 |
| 状态 | Draft，待审核 |

本次只解决一个问题：把现有 RBAC 管理能力收敛到新的 `iam` 包下面。它不是权限模型重做、API 重做、数据库重做，也不是 IdP 或 JWT 改造。

## 2. 当前实现与迁移目标

当前 RBAC 管理能力以多个顶层业务包存在。它们已经有各自的 Controller、Facade/Service、Repository、DTO、VO、PO、枚举和测试，但代码包路径没有体现它们属于同一个 IAM 管理域。

| 能力 | 当前包 | 当前 Controller | 代表模型/能力 | 迁移后包 |
| --- | --- | --- | --- | --- |
| 租户 | `admin.tenant` | `TenantController` | `TenantPO`、租户上下文、租户状态 | `admin.iam.tenant` |
| 用户/身份成员关系 | `admin.identity` | `UserDirectoryController`、`InternalIdentityController` | `UserPO`、身份与租户成员关系 | `admin.iam.identity` |
| 角色、权限绑定、继承 | `admin.role` | `RolePermissionController` | `RolePO`、`RolePermissionPO`、`RoleInheritancePO`、`RoleClosurePO` | `admin.iam.role` |
| 应用、资源、权限、字段定义、Manifest | `admin.resource` | `ApplicationResourceController`、`ManifestController` | `ApplicationPO`、`ResourcePO`、`PermissionPO`、`PermissionResourcePO`、`FieldDefinitionPO`、`ResourceManifestPO` | `admin.iam.resource` |
| 组织、岗位、目录快照 | `admin.directory` | `DirectoryController` | `DirectorySnapshotPO`、`OrgUnitPO`、`PositionPO`、`UserPositionSnapshotPO` | `admin.iam.directory` |
| 约束与数据/字段策略 | `admin.constraint` | `ConstraintController` | `DataRulePO`、`DataRuleReferencePO`、`FieldRulePO`，以及现有 SOD、前置条件、基数约束 | `admin.iam.constraint` |
| 用户角色分配 | `admin.assignment` | `AssignmentController` | `UserRoleAssignmentPO`、`AutoAssignmentRulePO` | `admin.iam.assignment` |
| 用户激活角色 | `admin.activation` | `RoleActivationController` | `UserActiveRolePO`、激活候选与重校验 | `admin.iam.activation` |

这里的 `resource` 保持当前聚合方式：`Application`、`Resource`、`Permission`、`FieldDefinition` 和 Manifest 仍属于同一现有子包；FIELD 仍由 `FieldDefinitionPO` 独立建模，不增加 `ResourceTypeEnum.FIELD`。

## 3. 目标包结构

本轮采用最小迁移结构，只在现有业务根包外增加一层 `iam`：

```text
top.egon.cola.platform.rbac3.admin
└── iam
    ├── tenant
    ├── identity
    ├── role
    ├── resource
    ├── directory
    ├── constraint
    ├── assignment
    └── activation
```

每个子包内部继续沿用当前 COLA 分层：

```text
iam/<capability>/
├── controller
├── domain
│   ├── dto
│   ├── enums
│   ├── exception
│   ├── po
│   └── vo
├── repository
└── service
```

实际存在的 `internal`、`jpa`、`jdbc`、`redis`、`scheduled` 等子包随所属能力整体迁移，不重新分层。

### 3.1 有意保留的名称

为避免把一次包迁移扩大成领域重命名，本轮保留当前名称：

- `identity` 暂不改名为 `user`；
- `constraint` 暂不改名为 `policy`；
- `resource` 暂不拆成 `application`、`resource`、`permission`；
- `directory` 暂不拆成 `organization`、`position`；
- `assignment`、`activation` 暂不并入 `role`。

这些名称调整可以作为后续独立变更，本 Spec 不提前引入兼容层或新抽象。

## 4. 迁移范围

### 4.1 纳入迁移

对下列旧包做递归迁移，包含生产代码、`package-info.java` 和对应测试代码：

```text
top.egon.cola.platform.rbac3.admin.tenant
    -> top.egon.cola.platform.rbac3.admin.iam.tenant
top.egon.cola.platform.rbac3.admin.identity
    -> top.egon.cola.platform.rbac3.admin.iam.identity
top.egon.cola.platform.rbac3.admin.role
    -> top.egon.cola.platform.rbac3.admin.iam.role
top.egon.cola.platform.rbac3.admin.resource
    -> top.egon.cola.platform.rbac3.admin.iam.resource
top.egon.cola.platform.rbac3.admin.directory
    -> top.egon.cola.platform.rbac3.admin.iam.directory
top.egon.cola.platform.rbac3.admin.constraint
    -> top.egon.cola.platform.rbac3.admin.iam.constraint
top.egon.cola.platform.rbac3.admin.assignment
    -> top.egon.cola.platform.rbac3.admin.iam.assignment
top.egon.cola.platform.rbac3.admin.activation
    -> top.egon.cola.platform.rbac3.admin.iam.activation
```

迁移内容包括：

1. Controller、Facade/Service、Repository 及其实现；
2. Domain key、DTO、Command、Query、VO、PO、Entity、Enum、Exception；
3. `package-info.java` 和当前能力对应的测试；
4. `admin.config` 中对上述类型的显式 import、Bean 装配和配置引用；
5. Admin 模块内部其他代码对上述包的 import。

仓库扫描未发现 Admin 模块之外对这些 `admin` 实现包的 Java 依赖；仍需在实现阶段以全仓库搜索结果为准更新所有调用方。

### 4.2 明确不纳入

下列包和能力本轮保持原路径和行为，不因为 `iam` 聚合而移动：

```text
admin.audit
admin.authorization
admin.bootstrap
admin.config
admin.management
admin.participation
admin.runtime
admin.simulation
admin.shared
```

其中：

- `admin.shared` 是跨业务域公共基础包，继续作为公共依赖，不复制为 `iam.shared`；
- `authorization`、`runtime`、`participation`、`simulation` 是运行时决策、投影、业务参与事实和模拟能力，不是本次管理域包聚合的对象；
- `management` 是管理者管理权限/委托策略能力，是否纳入 IAM 需要另行确定，不能在本轮隐式扩大范围；
- `bootstrap`、`config`、`audit` 保持平台装配、启动、审计边界不变；
- 不移动任何 session、JWT、IdP、Gateway 或 Starter 代码。

## 5. 不变的运行时和业务契约

### 5.1 HTTP API 不变

包名变化不等于 URL 变化。本轮不增加 `/iam` URL 前缀，也不重构 REST 语义、DTO 或响应结构。当前 Controller 的 `@RequestMapping` 和方法映射原样保留，例如：

- `/api/rbac3/v1/platform/tenants`、`/api/rbac3/v1/users`；
- `/api/rbac3/v1/roles` 及角色权限/继承操作；
- `/api/rbac3/v1/applications`、资源 Manifest；
- `/api/rbac3/v1/org-units`、`/api/rbac3/v1/positions`；
- `/api/rbac3/v1/data-rules`、`/api/rbac3/v1/field-rules`；
- `/api/rbac3/v1/users/{userId}/role-assignments`；
- `/api/rbac3/v1/auth/role-activation-candidates`、`/api/rbac3/v1/auth/role-activations`；
- 现有 `/internal/...` 路由。

附件中提出的 CRUD 补全、`add/replace/remove` 语义化接口、关系表 API 收敛和组合查询，作为后续 API 设计任务，不在本轮 package 迁移中实现。

### 5.2 持久化模型不变

- 不新增、删除或修改 Flyway migration；
- `@Entity` 名称、`@Table`、列名、索引、唯一约束和实体字段保持不变；
- 不修改 `TenantScopedPO`、`tenantId`、版本字段和审计字段语义；
- 不改变 `UserPO` 当前仅保存 IdP `identitySub` 等 RBAC 侧用户对象的边界，不新增密码、用户资料、Session 或 Refresh Token；
- `RoleClosurePO`、`UserActiveRolePO`、`DataRuleReferencePO` 等关系/派生结构继续保留；
- Application 维度、Tenant 维度、角色激活、数据规则和字段规则的现有校验继续由原服务和仓储负责。

### 5.3 Spring 和分层契约不变

`Rbac3AdminApplication` 位于 `admin` 根包，当前使用 `@SpringBootApplication` 的默认子包扫描，源码中没有额外的 `@EntityScan`、`@EnableJpaRepositories` 或自定义组件扫描根。新增 `admin.iam.*` 后应继续被默认扫描，不新增扫描配置。

迁移后仍需遵守现有分层约束：Controller 依赖本能力的 domain/service，Service 不直接依赖实现层 Repository，Repository 不依赖 Controller/Service。不要因为增加 `iam` 而引入新的通用 `BaseService`、Facade、Factory 或跨域反向依赖。

## 6. 实施方案

### 阶段 A：冻结迁移清单

1. 以当前源码为准记录八个纳入包的文件清单、Controller 映射、Bean 和测试；
2. 搜索全仓库旧包名引用，确认无模块外 Java 调用方；
3. 记录实体注解和现有 HTTP 映射，作为迁移后的等价性基线。

### 阶段 B：执行包迁移

1. 按旧包到新包映射整体移动目录；
2. 修改每个 Java 文件的 `package` 声明；
3. 批量更新 Admin 源码、测试和 `config` 中的 import；
4. 保留类名、方法名、注解、Bean 定义和 URI，不顺便重命名业务 API；
5. 保留 `package-info.java` 的文档内容，仅更新包名及包路径描述。

### 阶段 C：清理与验证

1. 旧八个根包下不再保留生产 Java 或测试 Java；
2. 全仓库不再出现旧实现包的 import/package 声明；
3. 检查 JPA Entity、Repository、Controller Bean 均能被加载；
4. 执行 Admin 模块编译和现有测试，重点覆盖架构边界、Controller、Repository、角色、约束、目录、租户和身份测试；
5. 对迁移前后的 `@RequestMapping`、实体注解和关键类名做差异检查。

## 7. 验收标准

### 7.1 结构验收

- 所有纳入类的包名均以 `top.egon.cola.platform.rbac3.admin.iam.` 开头；
- `iam` 子包仍保留当前 `controller/domain/repository/service` 层次；
- 每个生产包继续具有 `package-info.java`；
- 现有架构测试的分层方向和“一文件一个顶层类型”约束仍通过。

### 7.2 行为验收

- 所有现有 Controller 路径、HTTP 方法、参数、响应和权限注解不变；
- 现有 Role/Permission/Resource/Field/DataRule/Assignment/Activation 行为不变；
- Tenant 和 Application 隔离校验不变；
- IdP JWT 校验、Gateway、RBAC Starter、Runtime Projection 和缓存行为不变；
- 不引入 Session、密码或 Refresh Token 存储。

### 7.3 数据验收

- 不产生新的数据库迁移文件；
- 不改变现有表、字段、Entity 名称和关系；
- 现有数据可以由迁移后的代码直接读取和写入。

## 8. 设计取舍与风险

本次是包路径迁移，不需要新增设计模式；继续使用现有的 Facade、Repository、Projection 和事务边界。引入新的领域 Facade、适配器或兼容 Controller 只会扩大变更面，因此不采用。

包路径是 Java 编译契约的一部分。本迁移会破坏仍引用旧全限定类名的外部源码或已编译消费者；实现阶段必须先完成全仓库引用更新，并在发布说明中标注这一点。HTTP 客户端和数据库数据不受该包名变更影响。

## 9. 后续独立任务（本 Spec 不实现）

以下内容可以在本迁移完成后单独立项：

1. 将 `identity`、`constraint`、`directory` 等子包改成更语义化的 `user`、`policy`、`organization/position`；
2. 将 `resource` 内部按 Application、Resource、Permission、FieldDefinition 进一步拆分；
3. 按管理页面补齐 CRUD、Assignment、replace/add/remove 和组合查询 API；
4. 评估 `UserPositionSnapshotPO` 是否需要独立的用户-组织关系模型；
5. 评估 `management` 是否纳入 IAM 管理域；
6. 任何权限执行、数据过滤、字段脱敏或 Token/Session 改造。


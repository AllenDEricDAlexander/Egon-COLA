# RBAC3 Admin IAM 领域重命名、API 与管理前端迁移 Spec（含 DDC Business/Application 边界）

## 1. 文档信息与本次决策

| 项目 | 内容 |
| --- | --- |
| 适用模块 | egon-cola-platform-rbac3-admin、egon-cola-platform-rbac3-admin-web；egon-cola-platform-dynamic-config-center-admin、egon-cola-platform-dynamic-config-center-admin-web、egon-cola-platform-dynamic-config-center-starter；egon-cola-component-rpc-ddc-adapter |
| 变更类型 | IAM 业务域重命名、Java package 迁移、管理 API 迁移与补全、管理前端同步迁移；DDC Business/Application 主数据边界与只读 RPC 契约补齐 |
| 目标根包 | top.egon.cola.platform.rbac3.admin.iam |
| 对外 API 根路径 | /api/rbac3/v1/iam |
| 内部 API 根路径 | /internal/v1/iam（服务间成员解析）；/api/rbac3/v1/iam/internal（现有管理服务内嵌内部提交） |
| 兼容策略 | 破坏式迁移；不保留旧 URI、旧 Java package 或旧前端路由的兼容层 |
| 当前状态 | Draft，待审核 |

本 Spec 替换上一版“仅在现有包外增加 iam 一层，且不改名、不改 URL”的定义。那一版与本次已确认的目标不一致，不能继续作为实施依据。

本次按 IAM 模型完成以下三件事：

1. 现有分散的 RBAC 管理能力迁入统一的 iam 根包，并按 user、business、application、resource、permission、organization、position、policy、role 等语义重新命名；
2. 明确 DDC 的 `Business -> Application` 两层目录为唯一主数据：Business/Application 的新增、修改、删除、启停继续由 DDC 管理；RBAC 不复制这两类主数据，也不提供或代理它们的 CRUD；
3. RBAC 通过 DDC RPC 查询和校验目录，并继续在 RBAC 内管理“用户可访问哪些 Business”和“用户通过哪些应用角色/权限可访问哪些 Application”的授权关系；
4. RBAC 管理 API 统一迁移到 /api/rbac3/v1/iam，补齐 RBAC 自身实体 CRUD 与授权语义接口；RBAC3 Admin Web 同步迁移页面、前端路由、API Client、表单与测试。

这不是 IdP、JWT、Session、Gateway 鉴权链路或 Starter 的改造。RBAC 侧 User 仍只是租户内授权成员；账户密码、用户资料、Access Token、Refresh Token 继续属于 IdP 边界。

## 2. 现状依据与必须保留的边界

### 2.1 当前后端能力分散情况

| 当前包 | 当前 Controller | 当前核心模型 | 迁移后的归属 |
| --- | --- | --- | --- |
| admin.tenant | TenantController | TenantPO | iam.tenant |
| admin.identity | UserDirectoryController、InternalIdentityController | UserPO | iam.user |
| admin.resource | ApplicationResourceController、ManifestController | ApplicationPO、ResourcePO、FieldDefinitionPO、PermissionPO、PermissionResourcePO、ResourceManifestPO | iam.application、iam.resource、iam.permission |
| admin.role | RolePermissionController | RolePO、RolePermissionPO、RoleInheritancePO、RoleClosurePO | iam.role |
| admin.assignment | AssignmentController | UserRoleAssignmentPO、AutoAssignmentRulePO | iam.role.assignment |
| admin.activation | RoleActivationController | UserActiveRolePO | iam.role.activation |
| admin.directory | DirectoryController | DirectorySnapshotPO、OrgUnitPO、PositionPO、UserPositionSnapshotPO | iam.organization、iam.position |
| admin.constraint | ConstraintController | DataRulePO、DataRuleReferencePO、FieldRulePO，以及 SOD、前置条件、基数约束 | iam.policy |

当前公开 Controller 的共同根路径是 /api/rbac3/v1；内部 Controller 使用 /internal/v1。迁移后保留这两个系统级前缀，在其后显式加入 /iam，不另起一套 API 版本。

### 2.2 不能因重命名而破坏的模型事实

- UserPO 只保存 RBAC 侧用户成员和 IdP identitySub 等最小授权信息；它不是 IdP 账户，不包含密码、邮箱、手机号、头像或 Refresh Token。
- ResourcePO.resourceType 只有 APP、MENU、ROUTE、ACTION、API。FIELD 继续由 FieldDefinitionPO 单独建模，不能增加 ResourceTypeEnum.FIELD。
- Application、Resource、Permission、Role 都有 applicationId 维度；Role-Permission、Permission-Resource 与 Role 继承必须校验同 Tenant、同 Application。岗位自动角色规则校验其 Position 与 Role 同 Tenant；Role 自身的 applicationId 决定其授权范围，Position 不伪造 applicationId。
- RoleInheritancePO 是直接继承关系，RoleClosurePO 是派生/查询优化数据。不能暴露 RoleClosure 的 CRUD。
- UserRoleAssignmentPO 有 assignmentType、状态、validFrom、validTo、sourceType、sourceId、reason、ticketNo 等授权证据字段；新的 API 必须保留这些语义，不能简化成无来源的 user-role 表操作。
- UserActiveRolePO 表示“用户当前实际激活的角色集合”，不等于用户已获得的全部角色。激活角色仅影响权限上下文的填充，未激活角色不会回查填入权限上下文。
- ResourcePO 与 FieldDefinitionPO 都有 sourceManifestId。PermissionResourcePO 没有该字段，但由 Manifest 物化器创建的 API 映射带 definitionSetId/gatewayOperationId 身份。两类 Manifest 派生数据都不能被普通人工 CRUD 覆盖。
- DirectorySnapshotPO 是不可变目录提交记录；现有 OrgUnitPO、PositionPO 是由该快照物化的当前目录数据，均要求 snapshotId 非空。UserPositionSnapshotPO 也要求 snapshotId、userId、positionId、orgUnitId 均非空。因此这些现有目录来源数据不能被伪装成“用户只归属组织”的人工成员关系，也不能直接假装成可编辑的手工组织/岗位。
- 现有 DataRulePO、DataRuleReferencePO、FieldRulePO 是策略配置/决策模型。当前范围不得把“可以配置数据规则、字段规则”表述为已经自动过滤数据或自动将字段置空/脱敏；自动执行仍须由业务服务或 PEP 显式接入。

### 2.3 跨域技术边界

admin.shared 目前被 runtime、authorization、management、audit、simulation 等非 IAM 包共同使用。因此：

- 现有 admin.shared 保持原路径，不迁入 iam.shared；
- iam.shared 仅预留给未来 IAM 专用的 DTO 校验、查询组装或常量；本轮不为了目录完整性复制公共基础类；
- admin.audit、admin.authorization、admin.bootstrap、admin.config、admin.management、admin.participation、admin.runtime、admin.simulation 保持原边界，但它们对已迁移类型的 import 必须更新；
- Rbac3AdminApplication 继续位于 admin 根包。当前默认 Spring 子包扫描能够覆盖 admin.iam，无需新增 EntityScan、EnableJpaRepositories 或新的组件扫描配置。

### 2.4 Business -> Application 的主数据与授权边界

当前 DDC 已经具备两层目录，且它是本次的唯一主数据来源：

| DDC 主数据 | 当前实体/关系 | 当前管理 API | 本次归属 |
| --- | --- | --- | --- |
| Business（DDC 现有命名为 Biz） | `DdcBizEntity`，`ddc_biz`；`id`、`bizCode`、`bizName`、`enabled` | `/api/v1/ddc/bizs` 的 list/page/detail/create/update/delete/enabled | DDC Admin |
| Application | `DdcAppEntity`，`ddc_app`；`id`、`bizCode`、`appCode`、`appName`、`enabled` | `/api/v1/ddc/apps` 的 list/page/detail/create/update/delete/enabled；列表可按 `bizCode` 过滤 | DDC Admin |

`DdcAppEntity.bizCode` 表示 Application 属于 Business。RBAC 不把这两个实体迁入自己的 JPA 模型，不直连 DDC 表，也不把 DDC 管理 REST API 包装成 RBAC 的写接口。DDC Admin Web 的 `BizsPage`、`AppsPage` 继续是这两类主数据的唯一 CRUD 页面。

当前 `DdcManagementService` / `DdcManagementRpc` 只覆盖配置、发布、实例和 scope binding 等能力，尚未提供 Biz/App 目录读取。因此本次必须在现有 DDC Management RPC 链路中补齐只读目录能力；不能假定 RBAC 已经能通过 RPC 读取 Business/Application。

职责必须严格分开：

| 事项 | DDC | RBAC |
| --- | --- | --- |
| Business/Application 名称、编码、归属、启停、删除 | 唯一写入者 | 只读 RPC 使用者 |
| 用户可访问哪些 Business | 不保存、不判断 | 唯一授权写入者 |
| 用户可访问哪些 Application，以及菜单、接口、数据、字段等下级权限 | 不保存、不判断 | 通过应用范围内的角色、权限、资源和用户角色分配管理 |
| 授权上下文中的有效 Business/Application 范围 | 提供目录有效性事实 | 计算并填充；失效目录不产生有效授权 |

本 Spec 中的 **Business** 专指 DDC `Biz` 这一层目录；它不等同于现有 `businessResource`、`BusinessParticipationPO` 或操作 SOD 规则中的业务对象。

## 3. 目标 Java 包结构与迁移映射

### 3.1 目标结构

~~~text
top.egon.cola.platform.rbac3.admin
├── iam
│   ├── tenant
│   ├── user
│   ├── business
│   ├── application
│   ├── resource
│   │   ├── field
│   │   └── manifest
│   ├── permission
│   ├── role
│   │   ├── assignment
│   │   ├── activation
│   │   └── inheritance
│   ├── organization
│   │   └── snapshot
│   ├── position
│   │   └── snapshot
│   ├── policy
│   └── shared
├── shared
├── audit
├── authorization
├── bootstrap
├── config
├── management
├── participation
├── runtime
└── simulation
~~~

每个 IAM 子域继续遵守项目已有的 controller、service、domain、repository 分层；domain 内原有的 dto、command、query、vo、po、enums、exception、assembler、jpa/jdbc/internal 等目录随职责迁移。不得为了目录形式引入新的 BaseService、通用 Facade 或跨域反向依赖。

### 3.2 精确迁移规则

| 原包/模型 | 目标包 | 说明 |
| --- | --- | --- |
| admin.tenant | admin.iam.tenant | TenantPO、TenantController 及租户服务/仓储/测试 |
| admin.identity | admin.iam.user | UserPO、用户成员 CRUD、内部身份成员解析 |
| 新增的 Business 目录 RPC 适配、用户 Business 访问授权 | admin.iam.business | 只读查询 DDC Business 目录，维护 RBAC 的 UserBusinessAccess；不承载 DDC Business CRUD |
| resource 中 ApplicationPO 及其应用服务 | admin.iam.application | ApplicationPO 保留为租户内 RBAC 应用授权范围，引用 DDC Application；不再把它当作 DDC Application 主数据 |
| DDC 的 DdcBizEntity、DdcAppEntity 及其 Controller/Web 页面 | 保持 DDC 现有包 | 继续由 DDC 管理，绝不迁入 RBAC 的 iam 包 |
| resource 中 ResourcePO、ResourceManifestPO、FieldDefinitionPO | admin.iam.resource、iam.resource.manifest、iam.resource.field | 资源、Manifest、字段仍有清晰关系，但不再与 Permission 混在一个业务包 |
| resource 中 PermissionPO、PermissionResourcePO | admin.iam.permission | Permission 与 Resource 分离；PermissionResource 作为 Permission 的配置关系 |
| admin.role | admin.iam.role | RolePO、RolePermissionPO、角色读写与继承查询 |
| admin.assignment | admin.iam.role.assignment | UserRoleAssignmentPO、AutoAssignmentRulePO；概念仍保留，但不再是 IAM 顶层子域 |
| admin.activation | admin.iam.role.activation | UserActiveRolePO、候选查询和当前用户激活角色设置 |
| admin.directory 中 OrgUnitPO、DirectorySnapshotPO | admin.iam.organization、iam.organization.snapshot | organization 替代 directory 的组织含义 |
| admin.directory 中 PositionPO、UserPositionSnapshotPO | admin.iam.position、iam.position.snapshot | position 单独成为管理对象；快照保持只读投影 |
| admin.constraint | admin.iam.policy | DataRulePO、DataRuleReferencePO、FieldRulePO、SOD、前置条件、基数约束 |

所有 production/test Java 文件的 package 声明、package-info.java、配置 import、架构测试、Controller 测试、Repository 测试、全仓库引用均必须随之更新。旧的 admin.identity、admin.directory、admin.constraint、admin.assignment、admin.activation 及 resource 的旧聚合路径不保留转发类。

## 4. 对外 API 规范

### 4.1 统一约定

- 公开 IAM API：/api/rbac3/v1/iam。
- 服务间身份成员解析 API：/internal/v1/iam。
- 现有挂在管理服务公开根路径下的内部提交 API：/api/rbac3/v1/iam/internal。它们不强行改为 /internal/v1，以保持当前 Manifest/目录快照的服务路由方式。
- DDC Business/Application 的管理 API 继续是 `/api/v1/ddc/bizs` 和 `/api/v1/ddc/apps`，不属于 `/api/rbac3/v1/iam`，也不迁移到 RBAC 服务。
- RBAC 对 DDC 目录只提供受权限保护的只读选择/查询视图，并通过 RPC 取数；它不以 RBAC API 转发 DDC 的 POST、PUT、DELETE 或 enabled 操作。
- 公开业务 API 的 tenantId 来自当前认证/租户上下文，不允许普通调用方通过 body/query 覆盖 tenantId。
- 租户平台管理接口仍按现有平台管理员模型使用目标租户头；前端仅对 /api/rbac3/v1/iam/tenants 路径注入 X-RBAC3-Target-Tenant，不能误加到所有 IAM 请求。
- 所有集合查询支持当前项目既有的分页、排序、筛选和统一响应封装；下拉选择场景可复用受限列表查询，不能为每个关系再增加裸表 API。
- PUT 表示整体更新或明确的 replace；POST 表示创建/add；DELETE 表示语义化解除/归档。批量接口的 add、replace、remove 语义必须在 Command 名称、接口说明和前端按钮中显式区分。
- 所有写操作携带/校验当前版本字段，沿用 TenantScopedPO 的审计和版本语义；并发冲突必须返回项目现有的冲突错误，而不能静默覆盖。
- 当前状态模型已有 ARCHIVED、DISABLED、SUSPENDED 等生命周期时，DELETE 映射为相应的归档/停用业务动作，不物理删除仍被审计、运行时或关系数据引用的记录。

### 4.2 旧 URI 到新 URI

| 当前 URI 族 | 迁移后 URI 族 | 说明 |
| --- | --- | --- |
| /api/rbac3/v1/platform/tenants | /api/rbac3/v1/iam/tenants | 租户管理 |
| /api/rbac3/v1/users、/internal/v1/identity | /api/rbac3/v1/iam/users、/internal/v1/iam/users | 用户成员管理和内部解析 |
| /api/rbac3/v1/roles | /api/rbac3/v1/iam/roles | 角色、权限、继承 |
| /api/rbac3/v1/applications | /api/rbac3/v1/iam/applications | 租户内 RBAC Application 授权范围；不是 DDC Application 主数据管理 |
| /api/v1/ddc/bizs、/api/v1/ddc/apps | 保持 DDC 原 URI | DDC Business/Application 主数据 CRUD 和状态管理，不创建 RBAC 映射 URI |
| /api/rbac3/v1/applications/{applicationId}/resources、/api/rbac3/v1/resources/{resourceId}/archive、/api/rbac3/v1/resource-manifests | /api/rbac3/v1/iam/applications/{applicationId}/resources、/api/rbac3/v1/iam/resources/{resourceId}、/api/rbac3/v1/iam/resource-manifests | 资源、字段、Manifest |
| /api/rbac3/v1/org-units、/api/rbac3/v1/positions | /api/rbac3/v1/iam/organizations、/api/rbac3/v1/iam/positions | 组织和岗位 |
| /api/rbac3/v1/data-rules、/field-rules 及约束路由 | /api/rbac3/v1/iam/policies/... | 数据/字段规则和高级约束 |
| /api/rbac3/v1/users/{userId}/role-assignments | /api/rbac3/v1/iam/users/{userId}/roles 及嵌套授权动作 | 不再把关联表作为主 API |
| /api/rbac3/v1/auth/role-activation-candidates、/role-activations | /api/rbac3/v1/iam/users/me/role-activation-candidates、/active-roles | 保持当前用户自服务边界 |
| /api/rbac3/v1/internal/directory-snapshots、/api/rbac3/v1/internal/resource-manifests | /api/rbac3/v1/iam/internal/directory-snapshots、/api/rbac3/v1/iam/internal/resource-manifests | 保持现有服务内嵌内部提交方式 |

不保留旧 URI 的 Controller alias、网关 alias 或前端 fallback。GatewayOperation、资源 Manifest、接口权限声明、前端调用和测试在同一变更中改到新 URI，避免“后端已迁、前端仍调用旧地址”的半迁移状态。

### 4.3 RBAC 自身实体的基础 CRUD

下表只列 RBAC 自己拥有的实体或授权范围。GET 集合接口同时承担分页/筛选查询；详情、创建、更新、删除和状态调整均为独立、可授权的管理动作。Business/Application 的**目录主数据**不在本表中：其 CRUD 由 DDC 的既有 API 和 DDC Admin Web 完成。

| 管理对象 | 基础 URI | 特殊规则 |
| --- | --- | --- |
| Tenant | /iam/tenants | 平台管理员接口；DELETE 采用 Tenant 现有关闭/归档语义 |
| User | /iam/users | 仅创建/维护 RBAC 成员和 identitySub 绑定；不创建 IdP 账户、不管理密码或资料 |
| Application 授权范围 | /iam/applications | 以已存在的 DDC Application 为输入，在当前 tenant 建立/管理 RBAC 授权范围；只允许 admit、查询、RBAC 本地状态调整和无依赖时撤销，禁止修改 DDC 的编码、名称、Business 归属、启停或删除 |
| Resource | /iam/resources | applicationId 必填；人工资源才可完整编辑 |
| FieldDefinition | /iam/resources/{resourceId}/fields，/iam/fields/{fieldId} | 属于资源；人工字段才可完整编辑 |
| Permission | /iam/permissions | applicationId 必填；与资源通过语义 API 配置 |
| Role | /iam/roles | applicationId 必填；不通过裸 RolePermission CRUD 配置权限 |
| OrgUnit | /iam/organizations | 支持树结构；仅 MANUAL 来源可 CRUD/移动，目录快照来源只读 |
| Position | /iam/positions、/iam/organizations/{orgId}/positions | 必须归属同 tenant 的组织；仅 MANUAL 来源可 CRUD |
| DataRule | /iam/policies/data-rules | 策略定义 CRUD，不声称自动执行数据过滤 |
| FieldRule | /iam/policies/field-rules | 策略定义 CRUD，不声称自动执行字段脱敏/置空 |

除 Application 授权范围和下表明确的关系/语义对象外，每类 RBAC 自有资源最少提供：

~~~text
POST   <collection>
GET    <collection>
GET    <collection>/{id}
PUT    <collection>/{id}
DELETE <collection>/{id}
PUT    <collection>/{id}/status
~~~

对于嵌套资源，资源自身详情、更新、删除使用其全局 id 路径；父级路径负责“在父级下创建/列出”。例如 POST /iam/resources/{resourceId}/fields 创建字段，GET /iam/fields/{fieldId} 查询字段详情。

### 4.4 业务语义 API

关联实体不产生裸的 XxxRelationController。接口以 User、Role、Permission、Organization、Position 等业务主体为入口；内部仍可使用现有关系 PO、Repository 和事务。

| 业务能力 | URI 与动作 | 语义与限制 |
| --- | --- | --- |
| DDC Business 目录选择 | GET /iam/catalog/businesses | 只读；RBAC 通过 DDC RPC 返回可见的 Business 目录，不提供 create/update/delete/enabled |
| DDC Application 目录选择 | GET /iam/catalog/businesses/{ddcBusinessId}/applications | 只读；返回该 Business 下的 Application，供 RBAC 授权范围和用户授权选择，不能跨 Business 伪造关联 |
| 建立 RBAC Application 授权范围 | POST /iam/applications:admit | body 只接受 `ddcApplicationId` 和 RBAC 本地展示/排序类配置；服务经 DDC RPC 取得其父 Business、编码、名称和 enabled 状态后建立本地范围，不能由请求伪造目录字段 |
| RBAC Application 授权范围状态 | GET /iam/applications；GET /iam/applications/{applicationId}；PUT /iam/applications/{applicationId}/status；DELETE /iam/applications/{applicationId} | 操作的是本地 RBAC 范围。DELETE 只在没有 Role/Permission/Resource 等依赖时允许；否则采用本地停用或返回冲突。任何动作均不调用 DDC 写 API |
| 用户 Business 访问授权 | GET/PUT /iam/users/{userId}/business-accesses | RBAC 直接维护用户对 DDC Business 的访问授权；PUT 为人工授权集合的整体 replace，记录有效期、状态、原因/工单等证据，并在写入前经 DDC RPC 校验目标仍存在且 enabled |
| 用户 Application 访问视图 | GET /iam/users/{userId}/application-accesses | 按 Business 归属分组展示用户在每个 Application 的直接/有效角色和有效状态；它不是第二张“用户-应用”授权表 |
| 用户直接角色 | GET /iam/users/{userId}/roles | 返回直接授权及其 assignmentType、状态、有效期、sourceType/sourceId；不把继承角色混入结果 |
| 用户有效角色 | GET /iam/users/{userId}/effective-roles | 返回直接、自动、继承等最终有效角色，并标明来源 |
| 追加用户角色 | POST /iam/users/{userId}/roles | AssignUserRolesCommand；支持单个/批量 role 项，保留有效期、原因、工单等字段；每个角色所属的 Application 必须处于当前 tenant 的有效 RBAC 范围，且用户已有其父 Business 的有效访问授权 |
| 替换人工角色 | PUT /iam/users/{userId}/roles | ReplaceUserRolesCommand；仅以本次管理范围内的人工授权集为 replace 对象，不删除自动/继承来源；同样校验 Business 访问授权与 Application 范围 |
| 解除用户角色 | DELETE /iam/users/{userId}/roles/{roleId} | RevokeUserRoleCommand；只可撤销可撤销的直接授权，来源不匹配或记录歧义时返回冲突 |
| 授权记录生命周期 | POST /iam/users/{userId}/role-assignments/{assignmentId}:suspend 或 :resume | 这是有状态授权证据的业务动作，不是 assignment 表 CRUD |
| 当前激活角色 | GET /iam/users/me/role-activation-candidates、GET/PUT /iam/users/me/active-roles | 使用 AuthenticationPrincipal 的当前用户；管理员不能借此接口任意写其他用户的 active roles |
| 用户访问组合视图 | GET /iam/users/{userId}/access-profile | 单次返回用户、Business 访问授权、Application 访问视图、组织、岗位、直接角色、自动角色、继承角色、当前激活角色、有效权限；它是 Query View，不新增聚合实体 |
| 角色直接权限 | GET /iam/roles/{roleId}/permissions | 只返回 RolePermission 直接配置 |
| 角色有效权限 | GET /iam/roles/{roleId}/effective-permissions | 包含继承路径计算的有效结果并注明来源 |
| 角色权限配置 | POST/PUT/DELETE /iam/roles/{roleId}/permissions... | GrantRolePermissionsCommand、ReplaceRolePermissionsCommand、RevokeRolePermissionCommand；PUT 为集合整体替换 |
| 角色继承 | GET /iam/roles/{roleId}/parents、GET /children、POST/DELETE /parents/{parentRoleId} | parent 是 senior role；变更维护 closure，拒绝环和跨应用关系 |
| Permission-Resource 配置 | GET /iam/permissions/{permissionId}/resources、GET /iam/resources/{resourceId}/permissions；POST/PUT/DELETE /iam/permissions/{permissionId}/resources... | PermissionResourcePO 仅为内部关系；PUT 支持幂等整体替换 |
| 资源字段 | GET/POST /iam/resources/{resourceId}/fields；GET/PUT/DELETE /iam/fields/{fieldId} | FIELD 仍为 FieldDefinition，不进入 ResourceTypeEnum |
| 用户组织成员 | GET /iam/users/{userId}/organizations；POST/DELETE /iam/users/{userId}/organizations/{orgId}；GET /iam/organizations/{orgId}/users | 支持“属于组织但无岗位” |
| 组织树 | GET /iam/organizations/tree、GET /iam/organizations/{orgId}/children、POST /iam/organizations/{orgId}/move | MoveOrganizationCommand 在服务层维护 parentId/path/depth 和环校验 |
| 用户岗位 | GET /iam/users/{userId}/positions；POST /iam/users/{userId}/positions；DELETE /iam/users/{userId}/positions/{positionId}；GET /iam/positions/{positionId}/users | AssignUserPositionCommand 的 body 显式包含 orgUnitId、primary、有效期；服务校验 position.orgUnitId 与请求 orgUnitId 一致 |
| 岗位自动角色 | GET/PUT /iam/positions/{positionId}/auto-roles | 仅管理 matchType=POSITION 的 AutoAssignmentRule；规则的创建、变更、失效必须可追溯到 sourceType/sourceId |
| 数据/字段策略应用关系 | GET /iam/roles/{roleId}/data-rules、GET /iam/permissions/{permissionId}/data-rules、GET/PUT /iam/roles/{roleId}/field-rules；GET/PUT /iam/policies/data-rules/{ruleId}/scope-references | DataRule/FieldRule 自身已持有 roleId、permissionId 等业务关系；DataRuleReference 表示 USER/DEPT/ORG/POSITION 范围引用，不暴露裸 CRUD |
| 高级授权约束 | /iam/policies/sod-sets、/iam/policies/role-prerequisites、/iam/policies/role-cardinality、/iam/policies/operation-sod-rules 等 | 现有 SOD、前置条件、基数约束能力迁入 policy，不删除或弱化 |

对所有 replace 接口，服务端必须在一个事务中计算 keep/add/remove、执行状态变化、维护授权版本/缓存失效；前端不能以多次 DELETE/POST 模拟一个整体保存。

## 5. 必要的数据模型与所有权规则

### 5.1 保持的表与实体

除本节明确的唯一 V6 schema 变更外，Java package 迁移不修改现有 @Entity 名称、@Table 名称、tenantId、审计字段、版本字段和当前业务状态机。TenantPO、UserPO、ApplicationPO、ResourcePO、FieldDefinitionPO、PermissionPO、PermissionResourcePO、RolePO、RolePermissionPO、UserRoleAssignmentPO、UserActiveRolePO、RoleInheritancePO、RoleClosurePO、AutoAssignmentRulePO、DirectorySnapshotPO、OrgUnitPO、PositionPO、UserPositionSnapshotPO、DataRulePO、DataRuleReferencePO、FieldRulePO 继续保留。

`ApplicationPO` 的保留不表示 RBAC 继续拥有 DDC Application 主数据。迁移后它的语义是“当前 tenant 内的 RBAC Application 授权范围”；`id` 仍是 Role、Permission、Resource 等 RBAC 内部关系的主键，DDC Application 只以外部引用进入该范围。

### 5.2 Business/Application：DDC 目录与 RBAC 授权模型

#### 5.2.1 唯一主数据与本地授权范围

DDC 是 Business 和 Application 的唯一主数据写入者；RBAC 只保存下列两类授权事实，不复制或维护 DDC 主数据：

| RBAC 模型 | 建议表/现有表 | 归属 | 含义 |
| --- | --- | --- | --- |
| ApplicationPO（保留并扩展） | `rbac3_application` | iam.application | 当前 tenant 接纳的 DDC Application 的本地授权范围。它承载本地 `applicationId`、RBAC 本地状态及资源/角色/权限关系，不是 DDC App 的副本或 CRUD 对象 |
| UserBusinessAccessPO（新增） | `rbac3_user_business_access` | iam.business | 当前 tenant 内某用户可访问某个 DDC Business 的直接授权证据；它是 Business 层的 RBAC 权限，不是 DDC 目录成员关系 |

`rbac3_application` 至少增加 `ddc_application_id` 与 `ddc_business_id` 两个不可由 RBAC 管理请求伪造的引用字段；`bizCode`、`bizName`、`appCode`、`appName` 如保留在本地，只能是 DDC RPC 返回的只读展示快照，不能在 RBAC API 中独立修改。唯一约束改为 `(tenant_id, ddc_application_id)`，以允许不同 Business 下出现相同的 `appCode`。DDC Application 的 `id` 是绑定的稳定身份；`Business + Application` 的人可读定位使用 DDC 返回的 `(bizCode, appCode)`，不再将单独的 `applicationCode` 当作跨 Business 的全局身份。

所有当前仅以 `(tenant_id, application_code)` 关联应用的 RBAC 表、查询和契约，在同一迁移中改为使用本地 `application_id` 作为授权关系键；至少覆盖服务主体/服务权限、Operation SOD 和业务参与记录等现有代码值关联。`applicationCode` 如仍出现在审计或展示 VO 中，只是由 `applicationId` 反查出的展示值。这样既不要求把 DDC Business 主数据复制到 RBAC，也不会在 DDC 的两层目录下把同名 `appCode` 误判为同一个应用。

#### 5.2.2 用户 Business 与 Application 权限如何留在 RBAC

用户权限的主语、授予、撤销、有效期、审计和最终权限上下文都仍属于 RBAC，具体规则如下：

1. **Business 访问权限**由 `UserBusinessAccessPO` 表达。它以 `(tenantId, userId, ddcBusinessId)` 为授权事实，包含状态、validFrom、validTo、来源、原因/工单、审计和版本字段；`PUT /iam/users/{userId}/business-accesses` 只 replace 人工来源，不删除其他来源的将来扩展空间。
2. **Application 访问权限**不新增第二张 `UserApplicationAccess` 表。现有 `UserRoleAssignmentPO -> RolePO.applicationId` 已是用户在 Application 范围内获得角色/权限的权威关系；管理员通过 `POST/PUT /iam/users/{userId}/roles` 配置它，`GET /iam/users/{userId}/application-accesses` 只是按 Business/Application 聚合后的授权视图。
3. 一项 Application 范围内的角色只有同时满足“本地 Application 授权范围有效、它对应的 DDC Business/Application 仍有效、用户具有该 Business 的有效访问授权、角色分配自身有效”时，才进入用户的 effective role、active role 候选和权限上下文。
4. 撤销/停用 Business 访问授权不物理删除该 Business 下的用户角色分配；它们变为不生效。再次授予时，仍需按照角色分配自身的状态和有效期重新判断。变更必须更新用户 `authVersion` 并走现有权限缓存/投影失效路径。

这保证“用户配置 Business 和 Application 权限”全部发生在 RBAC，同时不在 RBAC 维护 Business/Application 的主数据 CRUD，也不制造两份用户-应用授权真相。

#### 5.2.3 DDC 只读 RPC 契约

在现有 `DdcManagementService`、`DdcManagementRpc`、`DdcManagementClient`、`RpcDdcManagementClient` 和 DDC provider 链路中，增加只读目录契约。最小方法集如下：

| RPC 方法 | 输入 | 输出/用途 |
| --- | --- | --- |
| `GetBiz` | `ddcBusinessId` 或 `bizCode` | `id`、`bizCode`、`bizName`、`enabled`；供 Business 授权写入校验和详情解析 |
| `ListBizs` | 关键字、enabled 等受限查询条件 | RBAC 选择器使用的 Business 列表 |
| `GetApp` | `ddcApplicationId` | `id`、`bizCode`、`appCode`、`appName`、`enabled`，以及其父 Business 的 `id`、`enabled`；供 Application 范围接纳和角色授权校验 |
| `ListApps` | `ddcBusinessId` 或 `bizCode`、关键字、enabled 等受限查询条件 | 指定 Business 下的 Application 列表 |

这些 RPC 方法只能读取 DDC 目录，不能携带 RBAC 的 User、Role、Permission 或授权写命令。RBAC 的 Application 范围接纳、Business 授权 replace、用户角色授予/replace、active-role 候选计算和权限上下文装配都必须使用该目录事实校验；缺失、跨 Business 或 disabled 的目录对象不得变成有效授权。允许使用缓存优化读取，但缓存只能是 DDC 目录的派生快照，不能成为独立写源，且失效时必须安全地拒绝把目标加入有效权限上下文。

### 5.3 组织和岗位的目录/人工来源

现有 OrgUnitPO、PositionPO 不是纯人工管理实体：两者的 snapshotId 非空，当前由 DirectorySnapshotMaterializer 根据 DirectorySnapshotPO 物化并更新。因此，若直接给它们增加普通 CRUD，会造成两种错误：

1. 人工新增记录被迫伪造 snapshotId；
2. 下一次目录快照把人工编辑覆盖或删除。

为同时保留目录同步和满足本次 Organization/Position CRUD 要求，V6 对现有 rbac3_org_unit、rbac3_position 增加明确的 sourceType（DIRECTORY_SNAPSHOT、MANUAL），并允许只有 MANUAL 记录的 snapshotId 为 null：

- migration 先将现有记录标记为 DIRECTORY_SNAPSHOT，再建立 sourceType 非空约束；既有目录数据仍保留其 snapshotId；
- DirectorySnapshotMaterializer 创建/更新 DIRECTORY_SNAPSHOT 记录，且只处理该来源；
- Organization/Position 的 POST、PUT、DELETE、状态和移动 API 只允许 MANUAL 记录；
- DIRECTORY_SNAPSHOT 记录在管理 CRUD 中只读；其唯一写入入口仍是目录快照提交；
- 返回 VO 和前端页面展示 source。目录来源记录禁用编辑/归档/移动按钮，并引导管理员走目录同步；
- 初始版本拒绝跨来源的组织结构改写，避免目录同步破坏人工层级；用户组织成员、岗位成员和岗位自动角色可引用任一仍有效的来源记录。

这不是用 null 暗示来源：sourceType 是明确语义，snapshotId=null 只在 sourceType=MANUAL 时合法。

### 5.4 用户组织/岗位关系的真实缺口

现有 UserPositionSnapshotPO 要求 positionId 与 snapshotId 非空，且用于外部目录快照。它不能安全表达：

~~~text
User -> OrgUnit
User -> OrgUnit -> Position（人工管理）
~~~

因此，为兑现本次已要求的“用户加入组织、无岗位组织成员、人工绑定岗位、组织成员/岗位成员查询”接口，本次新增以下两个管理关系模型：

| 新模型 | 建议表 | 归属 | 用途 |
| --- | --- | --- | --- |
| UserOrganizationAssignmentPO | rbac3_user_org_assignment | iam.organization | 人工/管理侧用户组织成员关系，可无岗位 |
| UserPositionAssignmentPO | rbac3_user_position_assignment | iam.position | 人工/管理侧用户-组织-岗位关系 |

实现时只新增一个 Flyway V6 migration：同时完成 5.2 的 DDC 引用/应用范围键迁移、`rbac3_user_business_access` 创建、5.3 的来源字段/约束调整，并创建本节的两个组织/岗位关系表及必要索引。两个组织/岗位关系模型均遵循 TenantScopedPO 的 tenant、版本、审计和状态规范，包含 userId、orgUnitId、有效期与状态；岗位关系额外包含 positionId、primary。服务层负责保证同租户、岗位归属组织一致、幂等重复提交和有效期合法，不以 positionId=null 或虚构 DirectorySnapshot 规避模型。

DirectorySnapshotPO 与 UserPositionSnapshotPO 保留为外部目录投影，只读展示/同步，不与人工管理关系混写。用户 access-profile 可合并展示“目录快照来源”和“人工来源”，并保留来源标记。

### 5.5 Manifest 与人工资源的所有权

ResourcePO、FieldDefinitionPO 与 PermissionResourcePO 存在由 Manifest 物化的情况。前两者的返回对象必须提供不新增列的派生 source：

~~~text
sourceManifestId == null  -> MANUAL
sourceManifestId != null  -> MANIFEST
~~~

PermissionResourcePO 没有 sourceManifestId；其 MANIFEST 来源由关联 Resource 的 sourceManifestId 以及 API 映射的 definitionSetId/gatewayOperationId 识别。手工映射不得伪装成 Manifest 映射。

- MANUAL 资源、字段及其允许的权限映射可以通过基础 CRUD/语义配置接口管理；
- MANIFEST 数据只允许读取、现有生命周期动作和重新提交/激活 Manifest；普通 PUT/DELETE/replace 返回业务冲突，不能覆盖源码声明；
- Permission-Resource 的批量 replace 也必须跳过/拒绝 Manifest 所有权数据，避免一次保存破坏物化结果。

### 5.6 自动角色、版本与缓存

AutoAssignmentRulePO 已可表达 POSITION 与 ALL_ACTIVE_USERS 匹配，但“有规则”不等于“已经可靠地将规则物化为用户授权”。本次要求增加明确的自动授权应用服务：

1. 用户人工绑定/解除岗位、岗位自动角色 replace、规则状态/有效期变化时，重新计算受影响用户；
2. 自动创建、撤销或失效对应的 UserRoleAssignmentPO，使用明确的自动 sourceType/sourceId；
3. 手工授权、自动授权、继承结果互不粗暴删除；撤销岗位只能清理由该岗位规则产生的授权；
4. 在同一事务中更新用户 authVersion、必要的 tenant policyVersion，并触发已有权限缓存/投影失效路径。

角色权限、角色继承、Permission-Resource、数据规则、字段规则和组织/岗位自动角色变化同样必须沿用现有授权版本与缓存失效机制。实现前先以现有调用链确认具体失效入口；不得只写库、不刷新权限决策数据。

## 6. 后端实现边界

### 6.1 Controller 与 Application Service

Controller 保持轻量：接收/校验 Command 或 Query、取得认证与租户上下文、调用 Application Service、返回当前统一响应。Controller 不直接写 Repository，不负责 closure/path 维护、授权差集计算或缓存失效。

推荐的职责拆分如下：

| 目标 Controller | 主要职责 |
| --- | --- |
| IamTenantController | Tenant 基础 CRUD、状态 |
| UserController、InternalUserController | User 成员 CRUD、访问组合查询、内部身份成员解析 |
| BusinessCatalogController、UserBusinessAccessController | 通过 DDC RPC 提供只读 Business/Application 选择器；维护用户 Business 访问授权。前者不写 DDC，后者不写 DDC 主数据 |
| ApplicationController | 当前 tenant 的 RBAC Application 授权范围接纳、查询、本地状态和无依赖撤销；不提供 DDC Application CRUD |
| ResourceController、FieldDefinitionController、ManifestController | Resource/Field CRUD、Manifest 读写与生命周期 |
| PermissionController | Permission CRUD 与 Permission-Resource 配置 |
| RoleController | Role CRUD、直接/有效权限、继承、角色字段/数据策略视图 |
| UserRoleController、RoleActivationController | 用户角色语义操作与当前用户激活角色操作 |
| OrganizationController、PositionController、DirectorySnapshotController | 组织树、组织成员、岗位、人工岗位关系、快照只读查询 |
| PolicyController 或按现有约束拆分的 Policy 子 Controller | DataRule、FieldRule、SOD、前置条件、基数等策略 |

已有类不是机械改名为一个巨型 IamController。原 Application/Domain Service、Repository、Assembler 保持单一职责并按新包归属移动；跨域编排只由业务动作所属的 Application Service 负责。DDC 目录访问收敛为 iam.business 中的单一 RPC adapter，不允许 Controller、Repository 或 JPA Entity 分散地访问 DDC REST/数据库。

### 6.2 Command、Query 与事务命名

新增或重命名的应用层动作使用业务语义，例如：

~~~text
AssignUserRolesCommand
ReplaceUserRolesCommand
RevokeUserRoleCommand
GrantRolePermissionsCommand
ReplaceRolePermissionsCommand
RevokeRolePermissionCommand
AddUserToOrganizationCommand
RemoveUserFromOrganizationCommand
AssignUserPositionCommand
RemoveUserPositionCommand
MoveOrganizationCommand
ReplacePositionAutoRolesCommand
ReplaceUserBusinessAccessesCommand
AdmitApplicationAuthorizationScopeCommand
~~~

不使用 SaveUserRoleDTO、UpdateRolePermissionDTO、InsertPermissionResource 等表操作命名。角色分配、角色权限 replace、Permission-Resource replace、组织移动、用户岗位、岗位自动角色和角色继承各自为事务边界。

不新增 Factory、Strategy、通用关系 Controller 等模式。当前直接的 Application Service + Repository + 领域对象已经适合 CRUD 与授权变更；唯一新增的“自动角色重算”使用专用领域/应用服务，是为了隔离其明确的来源与失效规则，而非建立通用规则引擎。

### 6.3 授权与隔离校验

每一个按 id 查询/修改的服务必须先在当前 tenant 范围内加载对象。写操作至少校验：

- User、Role、Permission、Resource、Application、OrgUnit、Position、Policy 均属于当前 tenant；
- Business 目录对象不属于 RBAC tenant，但每一条 `UserBusinessAccess` 和 Application 授权范围必须属于当前 tenant，并且引用 DDC RPC 返回的存在、归属正确且 enabled 的 Business/Application；
- Role-Permission、Permission-Resource 与 RoleInheritance 处于同一 applicationId；自动角色规则中的 Role 与 Position 必须同 Tenant，且角色沿用自身 applicationId 的授权范围；
- 用户角色写入、有效角色计算和 active-role 候选计算都必须同时校验该角色的 Application 授权范围、其父 Business 访问授权和 DDC 目录状态；不满足时不得进入权限上下文；
- 角色继承无环，RoleClosure 仅作为维护结果；
- Position 属于指定 OrgUnit；
- MANIFEST 来源数据不可被人工 CRUD 覆盖；
- 状态为失效/归档的上游对象不会被当作新的可授权对象；
- 当前用户激活角色的候选集只来自其已获授权且仍有效的角色。

### 6.4 DDC RPC 扩展与依赖方向

本次不新增 RBAC 到 DDC 的 HTTP Client，也不让 RBAC 读取 `ddc_biz`、`ddc_app` 数据库表。实现必须沿用现有 DDC Management RPC 链路，并在同一契约变更中同步以下位置：

1. `egon-cola-component-rpc-ddc-adapter` 的 `ddc_management.proto`、生成契约和 `DdcManagementRpc`；
2. DDC starter 的 `DdcManagementClient`；
3. RPC adapter 的 `RpcDdcManagementClient`、mapper/异常映射和 client factory；
4. DDC Admin 的 `DdcManagementRpcProvider`，由 `DdcBizService`、`DdcAppService` 提供只读实现；
5. RBAC Admin 的 iam.business RPC adapter 与其 Controller/Application Service。

依赖方向固定为 `RBAC IAM -> DDC Management RPC -> DDC Admin service`。DDC 不依赖 RBAC 的 User、Role、Permission 或其数据库；新增 RPC 也不包含 DDC Business/Application 的写方法。目录查询失败、找不到或目录状态无效时，RBAC 的授权写操作必须失败，权限上下文不能把该范围标记为有效。

## 7. RBAC3 Admin Web 迁移与功能范围

### 7.1 前端目录与路由

现有 tenant、application、assignment、directory、role、constraint、role-activation feature 按同一语义迁至：

~~~text
src/features/iam/
├── tenant
├── user
├── business
├── application
├── resource
├── permission
├── role
├── organization
├── position
├── policy
└── shared
~~~

前端技术公共层（例如 FeatureApi、adminApiClient、认证和查询基础设施）不因业务重命名而重复造一份。现有非 IAM 的 audit、runtime、simulation、management-policy、overview 页面保持当前 feature 边界。

管理页面路由同步改为：

~~~text
/iam/tenants
/iam/users
/iam/businesses
/iam/applications
/iam/resources
/iam/permissions
/iam/roles
/iam/organizations
/iam/positions
/iam/policies/data-rules
/iam/policies/field-rules
~~~

`/iam/businesses` 是 DDC Business/Application 的只读选择与授权入口，不是 RBAC 版本的 Business CRUD 页面；`/iam/applications` 是 RBAC Application 授权范围页面，不是 DDC Application CRUD 页面。DDC Admin Web 的 `BizsPage`、`AppsPage` 保持现有 DDC CRUD 职责，RBAC3 Admin Web 不复制它们。

Manifest、目录快照、角色权限/继承、用户访问画像等作为以上页面的详情/子页面，而不是继续散落在旧的 directory、assignment、constraint 路由组下。

### 7.2 页面与交互要求

| 页面/详情区域 | 必须支持的能力 |
| --- | --- |
| Tenant、Resource、Permission、Role、Organization、Position、DataRule、FieldRule | 列表/分页、筛选、详情、新增、编辑、归档/状态调整 |
| Business catalog | 只读展示 DDC Business 及其下 Application；作为用户 Business 授权和 RBAC Application 授权范围接纳的受限选择器；没有新增、编辑、删除、启停控件 |
| Application authorization scope | 已接纳 Application 的列表/详情、按 Business 筛选、接纳、本地启停和无依赖撤销；展示 DDC 来源及链接/跳转信息，不编辑 DDC 编码、名称、Business 归属或 enabled |
| User | RBAC 成员 CRUD、identitySub 展示、状态；不展示或编辑虚构的 IdP profile 字段 |
| User detail / access profile | Business 访问授权、按 Business/Application 分组的应用访问视图、组织、岗位、直接/自动/继承/有效角色、当前激活角色、有效权限的组合视图 |
| User roles | 追加、批量追加、replace、解除、暂停/恢复；只显示用户拥有 Business 访问授权且已接纳的 Application 的角色，页面清楚标出来源和有效期 |
| Role detail | 直接权限、有效权限、父/子角色、影响分析、字段/数据策略 |
| Permission detail | 绑定资源的 add/replace/remove 与资源反向查看 |
| Organization | 树、子组织、移动、组织成员、组织下岗位 |
| Position | 岗位成员、用户岗位绑定/解除、自动角色 replace |
| Resource detail | ResourceType、字段定义、MANUAL/MANIFEST 来源；Manifest 数据禁用普通编辑并给出来源说明 |
| Policy | DataRule、FieldRule 的 CRUD 和绑定视图；页面不得宣称已自动对业务返回数据脱敏/过滤 |
| My active roles | 当前登录用户的候选、查看和设置；不是任意用户管理页 |

所有选择类配置页面使用 Business/Application、角色/权限/资源/组织/岗位的受限选择器，不允许继续让管理员通过逗号分隔 id 文本框提交关系。Business/Application 选项来自 RBAC 后端的只读 DDC RPC 查询，不允许前端伪造 `bizCode`、`appCode` 或 DDC id。replace 保存调用单个 PUT 接口并在成功后使相关 React Query 缓存失效。

### 7.3 API Client、导航与测试同步

- business.api、application.api、role.api、assignment.api、directory.api、constraint.api、tenant.api、roleActivation.api 及其测试改到新的 iam API Client/路径；其中 business/application client 只调用 RBAC 的目录读取与授权范围/授权关系接口，绝不调用 DDC 主数据写接口；
- FeatureApi 中目标租户头的路径判断由 /api/rbac3/v1/platform/ 改为精确的 /api/rbac3/v1/iam/tenants，防止向普通租户 IAM 请求错误注入平台目标租户头；
- navigation.ts、governance.routes.tsx、集成测试的菜单权限和 URL 断言全部切换到 /iam 页面路由；
- 现有 directory.api 与后端 URI 不一致的调用在本次以新的 IAM 契约统一，不保留旧地址的特殊分支；
- 现有 RolePermissionPage 的逗号分隔权限编辑替换为可选择的直接权限 add/replace/remove，分别展示 direct 与 effective；
- 每个 CRUD 页面至少有 API Client 单测和组件交互测试；Business 授权 replace、Application 授权范围接纳、用户角色的 Business 前置校验、关键组合页、组织移动、岗位自动角色和 MANIFEST 只读限制补充端到端/集成覆盖。

## 8. 破坏式迁移与实施顺序

用户已确认不需要兼容旧数据/旧 API，因此本次不创建旧 URI alias、旧包 wrapper 或旧前端路由重定向。仍应保证一次发布中的后端、Gateway/Manifest、Admin Web 同步完成。

实施按以下顺序进行：

1. 冻结当前 Controller URI、GatewayOperation/资源声明、实体注解、前端调用、DDC Biz/App API 和测试清单；
2. 先扩展 DDC Management 的只读 Biz/App RPC 契约、provider、client adapter 和契约测试；DDC `/api/v1/ddc/bizs`、`/api/v1/ddc/apps` 及 DDC Admin Web CRUD 保持其主数据职责；
3. 新增唯一的 Flyway V6：建立 DDC 引用的 RBAC Application 授权范围键、`rbac3_user_business_access`、用户组织和人工用户岗位关系，并把既有 code-only 应用关系切换为本地 `applicationId`；
4. 先完成 Java package/类职责迁移及全仓库 import 更新，再实现/迁移 Controller 与 Command/Query；
5. 实现 RBAC 自有基础 CRUD、Application 授权范围接纳、Business/用户/角色授权语义、所有权校验、授权版本/缓存失效和自动角色重算；
6. 同步更新 GatewayOperation、资源 Manifest、权限声明和 RBAC3 Admin Web IAM feature；Business/Application 的主数据写页面仍在 DDC Admin Web；
7. 删除旧 RBAC URI、旧 Java package、旧 Web route/API 调用，执行全仓库旧路径搜索；不删除 DDC 的 Biz/App URI；
8. 进行 DDC RPC、后端、前端、迁移和跨域授权契约回归验证。

## 9. 验收标准

### 9.1 结构与契约

- 目标领域模型均位于 top.egon.cola.platform.rbac3.admin.iam 下的对应子域；旧顶层业务包不再保留实现类；
- identity 改名为 user，constraint 改名为 policy，directory 拆为 organization/position，resource 拆为 business/application/resource/permission，assignment/activation 并入 role 的子包；
- admin.shared 仍是跨域技术公共包；没有为了目录形式错误搬迁非 IAM 依赖；
- 所有公开 **RBAC IAM** 管理 URI 使用 /api/rbac3/v1/iam；服务间成员解析使用 /internal/v1/iam，Manifest/目录快照的内嵌内部提交使用 /api/rbac3/v1/iam/internal；旧 RBAC URI 不可再被 Controller、前端或 Gateway 清单引用。DDC Business/Application 继续使用 `/api/v1/ddc/bizs`、`/api/v1/ddc/apps`；
- DDC 是 Business/Application 的唯一主数据写入者；RBAC 只经 DDC Management RPC 读取/校验目录，不读 DDC 表、不代理 DDC 主数据写操作；
- RBAC 的 `ApplicationPO` 是 tenant 内授权范围并引用 `ddcApplicationId`/`ddcBusinessId`；`UserBusinessAccessPO` 是用户 Business 权限的唯一 RBAC 授权记录，用户 Application 权限由现有用户角色分配在该范围内表达；
- API 不暴露 RoleClosure、PermissionResource、RolePermission、DataRuleReference、UserPositionSnapshot 等裸关联表 CRUD。

### 9.2 业务行为

- RBAC 自有 Tenant、User、Resource、FieldDefinition、Permission、Role、Organization、Position、DataRule、FieldRule 等对象具备约定的 CRUD、列表/分页和状态操作；Application 授权范围具备接纳、查询、本地状态和无依赖撤销语义；Business/Application 主数据 CRUD 只在 DDC；
- 管理员可以在 RBAC 为用户 replace Business 访问授权，并通过 RBAC 的用户角色配置 Application 权限；二者均不调用 DDC 写 API；
- 用户的 Application 角色/权限只有在对应 Business 授权、本地 Application 授权范围和 DDC Business/Application 状态都有效时才进入 effective role、active role 候选和权限上下文；撤销 Business 访问不删除已有角色记录，但立即使其不生效并刷新授权版本；
- 角色、权限、资源、字段、组织、岗位、策略保持 Tenant 与 Application 隔离；
- 用户可单独加入组织而不要求岗位；岗位关系显式包含组织并校验一致性；
- 用户角色、角色权限、Permission-Resource 的 add/replace/remove 语义清楚、幂等并可审计；
- direct role/direct permission 与 effective role/effective permission 不混淆；
- 角色继承拒绝环，RoleClosure 不直接暴露；
- MANIFEST 数据不可被人工 CRUD 或 replace 覆盖；
- 自动角色仅处理与其 source 对应的 UserRoleAssignment，不误删人工或其他规则来源的角色；
- 激活角色仍仅由当前认证用户设置，且不改变“未激活角色不进入权限上下文”的现有约束；
- 数据/字段规则可配置，但没有未验证的自动执行承诺。

### 9.3 验证

- 后端：编译、架构测试、Controller/Service/Repository 测试、Flyway migration 校验、DDC Management RPC proto/provider/client 契约测试，以及关键授权事务、跨 tenant/Business/Application 拒绝测试；
- 前端：RBAC3 Admin Web 与 DDC Admin Web 的 TypeScript typecheck、单元/组件测试、构建；覆盖 DDC 主数据 CRUD、RBAC Business 授权 replace、Application 授权范围接纳、用户角色的 Business 前置校验和关键增强操作；
- 契约：GatewayOperation、资源 Manifest、权限声明、Admin Web API Client 和 Controller Mapping 的 URI 完全一致；
- 清理：全仓库搜索不再出现旧 IAM 业务 package、旧公开 URI 或旧前端路由引用（历史文档和迁移说明除外）。

## 10. 明确不在本次范围

- IdP 用户中台、密码、账户冻结、JWT 签发/验签、Refresh Token、Session、SSO、Gateway 认证流程的再设计；
- 将 DDC Business/Application 主数据复制到 RBAC、让 RBAC 直连 DDC 数据库、让 RBAC 代理 DDC 的主数据写 API，或把用户/角色授权关系写入 DDC；
- 为 Application 权限再引入独立 `UserApplicationAccess` 表；现有用户角色分配仍是 Application 范围用户授权的权威来源；
- 将 FIELD 强行并入 ResourceTypeEnum；
- 自动将 DataRule/FieldRule 注入任意业务查询或响应字段的 PEP/AOP 执行改造；
- admin.management、admin.runtime、admin.authorization、admin.audit 等非 IAM 管理业务的重新分包；
- 为旧 API、旧前端路由或旧 Java 全限定类名提供兼容层。

本 Spec 的重点是把已经存在的 RBAC 管理模型按 IAM 业务语言重新组织，在不夺取 DDC Business/Application 主数据职责的前提下补齐 RBAC 的用户范围授权、管理页面和语义配置能力；不借此创造另一套账户体系、资源模型或通用规则框架。

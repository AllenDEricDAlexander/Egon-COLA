# DDC 作用域模型修订实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 确立 biz→app→ns 层级 + env 独立实体的作用域模型；注册身份改为 biz-ns-env-app；实现四实体管理面（CRUD/筛选/禁用/保护性删除）与禁用门控（注册/拉取被拒）；服务注册页重构为 APP 维度 + Drawer 看实例（删除实例页）。

**Architecture:** 后端 V5 迁移新增 ddc_biz/ddc_env 表、ddc_app 加 biz_code、ddc_namespace 去 env；四实体统一 CRUD 模式；starter 的 `DdcServiceKey` 增加 bizCode/appCode（SDK 消费者零改动，配置驱动）；注册/拉取路径经 5s 缓存校验四实体 enabled。前端四管理页 + 服务注册页重构 + 级联下拉链 biz→app→ns + env 独立下拉。

**Tech Stack:** Spring Boot 3.5 + JPA + Flyway（postgresql/sqlite 双份迁移）；starter SDK（DdcServiceKey 契约）；React 19 + antd 6 + Vite + Vitest。

## Global Constraints

- 路径缩写：`<admin>/` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/`；`<starter>/` = `.../egon-cola-platform-dynamic-config-center-starter/`；`<web>/` = `.../egon-cola-platform-dynamic-config-center-admin-web/`。
- 注册身份恒为 biz-ns-env-app：`DdcServiceKey` 增加 `bizCode`、`appCode`（构造必填）；canonical 序列化含两字段；Redis 注册/实例/catalog key 含 biz/app。
- SDK 消费者零改动：appCode 取 `DdcProperties.appCode`（默认 default-app）；bizCode 新增配置 `egon.cola.component.ddc.biz-code`（`DdcProperties.bizCode`），注册时缺失抛错拒绝（错误提示"DDC biz-code is required"）。
- 禁用门控：`biz/app/ns/env` 任一 `enabled=false` → 注册 `register` 与配置拉取 `pull` 返回 `DDC_SCOPE_DISABLED`（ResultRecord failure，DdcErrorStatus 新增枚举）；心跳/续租不校验；四实体状态 5 秒本地缓存。
- 删除保护：biz 下有 app → `DDC_BIZ_IN_USE`；app 下有 ns → `DDC_APP_IN_USE`；ns 下有配置（任意 env）→ `DDC_NAMESPACE_IN_USE`；env 有配置引用或 Redis 注册 catalog → `DDC_ENV_IN_USE`。无子数据才可删；禁用不删数据。
- ns 建模：`ddc_namespace` 去 env 列，(app_code, namespace) 唯一；新建命名空间无 env。
- env 建模：`ddc_env` 表（env_code 唯一 + description + sort_order + enabled），前端环境下拉来自 `GET /api/v1/ddc/envs`；删除前端 `ENV_OPTIONS` 常量。
- 旧端点移除：`GET /namespaces/domains`；`GET /apps?namespace=`。
- 前端菜单 8 项：业务域、环境、服务注册、配置管理、应用、命名空间、发布任务、缓存。
- 服务注册页主列表 = 有实例的 APP；点击 APP 行 → Drawer 按服务分组展示实例。
- 空库兜底保留：下拉可输入新值；自动创建 app 归属当前选中 biz；自动创建 ns 挂 app 下；biz 不自动创建。
- 迁移双份：`db/postgresql/V5__*.sql` 与 `db/sqlite/V5__*.sql`（sqlite 去列需重建表）。
- 测试风格：后端 controller 用 `@WebMvcTest` + `@MockBean`；迁移用 `DdcV4MigrationTest` 模式；前端 vitest（jsdom 下 antd 下拉点击不可达，交互走输入路径，见既有 ScopeSelects 测试注释）。

---

### Task 1: V5 迁移（biz/env 表、app 加 biz、namespace 去 env）

**Files:**
- Create: `<admin>/src/main/resources/db/postgresql/V5__add_biz_env_and_detach_namespace_env.sql`
- Create: `<admin>/src/main/resources/db/sqlite/V5__add_biz_env_and_detach_namespace_env.sql`
- Modify: `<admin>/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV4MigrationTest.java`（或新建 `DdcV5MigrationTest`，沿用既有模式）

**Interfaces:**
- Consumes: V1-V4 已应用的 schema。
- Produces: 四实体目标 schema（见 SQL）；迁移可重复执行于空库与已有数据库。

- [ ] **Step 1: 写 postgresql 迁移**

```sql
create table ddc_biz (
    id varchar(64) primary key,
    biz_code varchar(128) not null,
    biz_name varchar(128) not null,
    description varchar(512),
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_env (
    id varchar(64) primary key,
    env_code varchar(32) not null,
    description varchar(256),
    sort_order int not null default 0,
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

insert into ddc_biz (id, biz_code, biz_name, description, enabled, created_at, updated_at)
values ('biz-default', 'default', '默认业务域', 'V5 迁移自动创建', true, now(), now());

insert into ddc_env (id, env_code, description, sort_order, enabled, created_at, updated_at)
values ('env-dev', 'dev', '开发环境', 10, true, now(), now()),
       ('env-test', 'test', '测试环境', 20, true, now(), now()),
       ('env-sit', 'sit', '集成环境', 30, true, now(), now()),
       ('env-gray', 'gray', '灰度环境', 40, true, now(), now()),
       ('env-prod', 'prod', '生产环境', 50, true, now(), now());

alter table ddc_app add column biz_code varchar(128);
update ddc_app set biz_code = 'default' where biz_code is null;
alter table ddc_app alter column biz_code set not null;

-- ddc_namespace 去 env：按 (app_code, namespace) 去重后重建
delete from ddc_namespace
 where id not in (
     select min(id) from ddc_namespace group by app_code, namespace
 );
drop index uk_ddc_namespace_key;
alter table ddc_namespace drop column env;
create unique index uk_ddc_namespace_key on ddc_namespace(app_code, namespace);

create unique index uk_ddc_biz_code on ddc_biz(biz_code);
create unique index uk_ddc_env_code on ddc_env(env_code);
```

- [ ] **Step 2: 写 sqlite 迁移（重建表法）**

```sql
create table ddc_biz (
    id varchar(64) primary key,
    biz_code varchar(128) not null,
    biz_name varchar(128) not null,
    description varchar(512),
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);

create table ddc_env (
    id varchar(64) primary key,
    env_code varchar(32) not null,
    description varchar(256),
    sort_order integer not null default 0,
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);

insert into ddc_biz (id, biz_code, biz_name, description, enabled, created_at, updated_at)
values ('biz-default', 'default', '默认业务域', 'V5 迁移自动创建', 1, datetime('now'), datetime('now'));

insert into ddc_env (id, env_code, description, sort_order, enabled, created_at, updated_at)
values ('env-dev', 'dev', '开发环境', 10, 1, datetime('now'), datetime('now')),
       ('env-test', 'test', '测试环境', 20, 1, datetime('now'), datetime('now')),
       ('env-sit', 'sit', '集成环境', 30, 1, datetime('now'), datetime('now')),
       ('env-gray', 'gray', '灰度环境', 40, 1, datetime('now'), datetime('now')),
       ('env-prod', 'prod', '生产环境', 50, 1, datetime('now'), datetime('now'));

alter table ddc_app add column biz_code varchar(128);
update ddc_app set biz_code = 'default' where biz_code is null;

-- sqlite 删列需重建表
create table ddc_namespace_new (
    id varchar(64) primary key,
    app_code varchar(128) not null,
    namespace varchar(128) not null,
    description varchar(512),
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);
insert into ddc_namespace_new (id, app_code, namespace, description, enabled, created_at, updated_at)
select id, app_code, namespace, description, enabled, created_at, updated_at
  from ddc_namespace
 where id in (select min(id) from ddc_namespace group by app_code, namespace);
drop table ddc_namespace;
alter table ddc_namespace_new rename to ddc_namespace;

create unique index uk_ddc_namespace_key on ddc_namespace(app_code, namespace);
create unique index uk_ddc_biz_code on ddc_biz(biz_code);
create unique index uk_ddc_env_code on ddc_env(env_code);
```

- [ ] **Step 3: 写迁移测试**

沿用 `DdcV4MigrationTest` 模式（Spring Boot 测试连 sqlite 内存库跑 flyway）。新建 `DdcV5MigrationTest.java`，断言：
- 新库迁移后存在 `ddc_biz`（含 `default` 行）、`ddc_env`（含 dev/test/sit/gray/prod 五行）、`ddc_app.biz_code` 非空（默认 default）、`ddc_namespace` 无 env 列。
- 造含重复 (app_code, namespace) 的旧数据跑迁移后仅保留最早行（测试内直接用 SQL 插入两行同 (app, ns) 不同 env，断言迁移后只剩 1 行）。

Run: `cd <admin> && mvn -q test -Dtest=DdcV5MigrationTest`
Expected: PASS。

- [ ] **Step 4: Commit**

```bash
git add <admin>/src/main/resources/db <admin>/src/test/java/.../DdcV5MigrationTest.java
git commit -m "feat(ddc-admin): V5 migration for biz env entities and namespace env detach"
```

---

### Task 2: biz 实体 + CRUD API

**Files:**
- Create: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcBizEntity.java`
- Create: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcBizRepository.java`
- Create: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/service/DdcBizService.java`
- Create: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcBizController.java`
- Create: `<admin>/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcBizControllerTest.java`

**Interfaces:**
- Consumes: Task 1 的 ddc_biz 表。
- Produces:
  - `DdcBizEntity`：`id, bizCode, bizName, description, enabled, createdAt, updatedAt`（字段命名与 DdcAppEntity 一致，Lombok Getter/Setter）。
  - `DdcBizRepository extends JpaRepository<DdcBizEntity, String>`：`Optional<DdcBizEntity> findByBizCode(String)`；`boolean existsByBizCode(String)`；`boolean existsByBizCodeAndIdNot(String, String)`；`List<DdcBizEntity> findByBizCodeContainingIgnoreCaseOrBizNameContainingIgnoreCase(String, String)`。
  - `DdcBizService`：`list(keyword)`、`findByBizCode`、`save(entity)`（bizCode 唯一校验，重复抛 `CommonException` 带 `DdcErrorStatus` 冲突码，用既有错误枚举或新增 `DDC_BIZ_CODE_EXISTS`）、`delete(bizCode)`（`appRepository.existsByBizCode` → 抛 `DDC_BIZ_IN_USE`）、`setEnabled(bizCode, enabled)`。
  - `DdcAppRepository` 增加 `boolean existsByBizCode(String bizCode)`。
  - `DdcBizController`：`@RequestMapping("/api/v1/ddc/bizs")`；`GET`（可选 `keyword` 模糊）、`GET /{code}`、`POST`、`PUT /{code}`、`DELETE /{code}`、`PUT /{code}/enabled?enabled=`。
  - `DdcErrorStatus` 增加：`DDC_BIZ_IN_USE`、`DDC_BIZ_CODE_EXISTS`、`DDC_SCOPE_DISABLED`、`DDC_APP_IN_USE`、`DDC_NAMESPACE_IN_USE`、`DDC_ENV_IN_USE`、`DDC_ENV_CODE_EXISTS`（T2 先用 biz 两个，其余随各任务加入）。

- [ ] **Step 1: 写失败的 controller 测试**（@WebMvcTest(DdcBizController.class) + @MockBean DdcBizService，用例：list 带 keyword 透传、POST 成功、DELETE 被占用抛错映射为 ResultRecord failure）

```java
@WebMvcTest(DdcBizController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcBizControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean DdcBizService bizService;

    @Test
    void listWithKeywordDelegatesToService() throws Exception {
        when(bizService.list("pay")).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/ddc/bizs").param("keyword", "pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.DDC_BIZ_IN_USE))
                .when(bizService).delete("pay");
        mockMvc.perform(delete("/api/v1/ddc/bizs/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.DDC_BIZ_IN_USE.code()));
    }
}
```

（`DdcErrorStatus` 的失败 JSON 形态以 `ResultRecord.failure(ErrorStatus)` 序列化结果为准，实施时按实际 `code()` 取值断言。）

- [ ] **Step 2: 运行确认失败** → `mvn -q test -Dtest=DdcBizControllerTest`（编译失败即可）。
- [ ] **Step 3: 实现实体/repository/service/controller**（按 Interfaces；`save` 唯一性用 `existsByBizCode` 前置检查 + 捕获 DataIntegrityViolation 兜底；`delete` 保护用 `existsByBizCode`；错误经 `CommonException` 由既有 `DdcGlobalExceptionHandler` 转 ResultRecord）。
- [ ] **Step 4: 运行测试确认通过** → PASS。
- [ ] **Step 5: Commit** `feat(ddc-admin): add biz entity CRUD with delete protection`

---

### Task 3: env 实体 + CRUD API

**Files:**
- Create: `<admin>/.../model/entity/DdcEnvEntity.java`、`repository/DdcEnvRepository.java`、`service/DdcEnvService.java`、`controller/DdcEnvController.java`
- Create: `<admin>/src/test/java/.../controller/DdcEnvControllerTest.java`
- Modify: `<admin>/.../repository/DdcConfigItemRepository.java`（加 `existsByEnv`）

**Interfaces:**
- Consumes: Task 1 的 ddc_env 表。
- Produces:
  - `DdcEnvEntity`：`id, envCode, description, sortOrder, enabled, createdAt, updatedAt`。
  - `DdcEnvRepository`：`findByEnvCode`、`existsByEnvCode`、`existsByEnvCodeAndIdNot`、`findAllByOrderBySortOrderAsc()`、`findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase`。
  - `DdcEnvService`：`list(keyword)`（无 keyword 时按 sortOrder 升序）、`save`（env_code 唯一）、`delete(envCode)`（保护：`configItemRepository.existsByEnv(envCode)` 或 Redis catalog key 存在 → `DDC_ENV_IN_USE`）、`setEnabled`。
  - `DdcConfigItemRepository.existsByEnv(String env)`。
  - `DdcEnvController`：`/api/v1/ddc/envs`，GET（keyword）/GET {code}/POST/PUT {code}/DELETE {code}/PUT {code}/enabled。
- Redis catalog 存在性检查（可选实现）：`RedissonClient.getKeys().getKeysByPattern(pattern)` 非空即占用；pattern 须匹配 T6 之后的 catalog key 结构（`ddc:registry:catalog:{biz}:{app}:{env}:{ns}:{kind}:{protocol}`），故用 `"ddc:registry:catalog:*:*:" + env + ":*"`。

- [ ] **Step 1-5**: 与 Task 2 同构（测试：list 排序、POST 成功、DELETE 被引用 → `DDC_ENV_IN_USE`）。
- [ ] **Step 6: Commit** `feat(ddc-admin): add env entity CRUD with delete protection`

---

### Task 4: app 实体改造（biz 归属）+ CRUD 完整化

**Files:**
- Modify: `<admin>/.../model/entity/DdcAppEntity.java`（+bizCode）、`repository/DdcAppRepository.java`、`service/DdcAppService.java`、`controller/DdcAppController.java`
- Modify: `<admin>/src/test/java/.../controller/DdcAppControllerTest.java`
- Create: `<admin>/src/test/java/.../controller/DdcAppAdminControllerTest.java`（如需要拆分）

**Interfaces:**
- Consumes: Task 1/2（biz 表、DdcBizRepository）。
- Produces:
  - `DdcAppEntity.bizCode`（String，必填）。
  - `DdcAppRepository`：移除 `findAllByAppCodeIn`（随 ?namespace= 移除）；增加 `List<DdcAppEntity> findByBizCode(String)`、`List<DdcAppEntity> findByBizCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(String, String)`、`boolean existsByAppCode`、`boolean existsByBizCode(String)`（T2 已加）。
  - `DdcAppService`：`list(bizCode, keyword)`（bizCode 可空=全部，keyword 对 appCode/appName 模糊——用 repository 派生查询组合，简单实现：bizCode 空时 `findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase`）、`save`（biz 必须存在，否则 `DDC_BIZ_NOT_FOUND`——DdcErrorStatus 新增）、`update(appCode, entity)`（biz 可改）、`delete(appCode)`（`namespaceRepository.existsByAppCode(appCode)` → `DDC_APP_IN_USE`；DdcNamespaceRepository 加 `existsByAppCode`）、`setEnabled`。
  - `DdcAppController`：`GET /api/v1/ddc/apps?biz=&keyword=`；移除 `namespace` 参数与 `findByNamespace`；新增 `PUT /{appCode}`、`DELETE /{appCode}`、`PUT /{appCode}/enabled`。
- 同时更新既有 `DdcAppControllerTest`（?namespace= 用例改为 ?biz=）。

- [ ] **Step 1: 改失败测试**（更新 DdcAppControllerTest：list 带 biz 透传；新增 DELETE 占用 → DDC_APP_IN_USE）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**（按 Interfaces；entity 加字段后既有构造/拷贝点同步）
- [ ] **Step 4: 运行测试确认通过** + `mvn -q test`（admin 全量，确认无其他引用 `findByNamespace`/`findAllByAppCodeIn`）
- [ ] **Step 5: Commit** `feat(ddc-admin): apps belong to biz with full CRUD`

---

### Task 5: ns 实体改造（去 env）+ CRUD 完整化

**Files:**
- Modify: `<admin>/.../model/entity/DdcNamespaceEntity.java`（-env）、`repository/DdcNamespaceRepository.java`、`service/DdcNamespaceService.java`、`controller/DdcNamespaceController.java`
- Modify: `<admin>/src/test/java/.../controller/DdcNamespaceControllerTest.java`

**Interfaces:**
- Consumes: Task 1 迁移后的表结构。
- Produces:
  - `DdcNamespaceEntity` 删除 env 字段；`DdcNamespaceRepository` 移除 `findByAppCodeAndEnv`、`findByAppCodeAndEnvAndNamespace`、`findDistinctAppCodesByNamespace`、`findDistinctNamespaces`（domains 逻辑）；新增 `Optional<DdcNamespaceEntity> findByAppCodeAndNamespace(String, String)`、`boolean existsByAppCode(String)`、`boolean existsByAppCodeAndNamespace(String, String)`、`List<DdcNamespaceEntity> findByAppCode(String)`、`List<DdcNamespaceEntity> findByAppCodeAndNamespaceContainingIgnoreCase(String, String)`、`boolean existsByAppCodeAndNamespaceAndIdNot(String, String, String)`。
  - `DdcNamespaceService`：`list(appCode, keyword)`（appCode 可空=全部；keyword 对 namespace 模糊）、`save`（(app, ns) 唯一）、`update`（ns 名可改，app 不可改）、`delete(id)`（`configItemRepository.existsByAppCodeAndNamespace(appCode, ns)` → `DDC_NAMESPACE_IN_USE`；DdcConfigItemRepository 加 `existsByAppCodeAndNamespace`）、`setEnabled`。
  - `DdcNamespaceController`：`GET /api/v1/ddc/namespaces?appCode=&keyword=`（无 env）；移除 `/domains`；`POST`（body 无 env）；新增 `PUT /{id}`、`DELETE /{id}`、`PUT /{id}/enabled`。
  - 安全配置：`DdcAdminSecurityConfiguration` 的 GET 清单移除 `/namespaces/domains`；`/apps` 保持。
- 受影响调用方同步：`DdcConfigController`/facade/`ConfigEditorDialog` 的 ensureAppAndNamespace 等前端在 T13/T15 处理；后端搜 `findByAppCodeAndEnv` 全量替换。

- [ ] **Step 1: 改失败测试**（更新 DdcNamespaceControllerTest：list 带 appCode+keyword；domains 用例删除；DELETE 占用 → DDC_NAMESPACE_IN_USE）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**（按 Interfaces；后端全仓 grep `findByAppCodeAndEnv|/domains|namespaces/domains` 清理）
- [ ] **Step 4: 运行测试确认通过** + `mvn -q test` 全量（修复受影响用例）
- [ ] **Step 5: Commit** `feat(ddc-admin): namespaces detach from env with full CRUD`

---

### Task 6: 注册契约（starter）增加 biz/app 维度

**Files:**
- Modify: `<starter>/.../model/registry/DdcServiceKey.java`、`.../common/DdcKeys.java`、`.../model/registry/DdcServiceRegistration.java`、`.../model/dto/DdcServiceLeaseRequest.java`、`.../model/registry/DdcServiceQuery.java`、`.../model/registry/DdcRegistryEvent.java`、`.../model/registry/DdcServiceSnapshot.java`、`.../registry/DdcServiceRegistryClient.java`
- Modify: `<starter>/.../config/DdcProperties.java`（+bizCode）
- Modify: `<admin>/.../repository/DdcServiceRegistryRedisRepository.java`、`service/DdcServiceRegistryService.java`、`service/DdcManagementFacade.java`、`controller/DdcRegistryAdminController.java`、`controller/DdcRegistryOpenApiController.java`
- Modify: 相关测试（starter 与 admin 两侧，按编译错误逐一更新构造点）

**Interfaces:**
- Consumes: 无（契约自身）。
- Produces:
  - `DdcServiceKey`：构造参数顺序改为 `(bizCode, appCode, env, namespace, serviceKind, serviceName, group, version, protocol)`；`canonicalValue()` 与 `parse()` 同步（biz、app 置于最前两行）；ORDER 比较器前两键为 bizCode/appCode。
  - `DdcProperties`：`private String bizCode;`（getter/setter；默认 null）。
  - `DdcServiceRegistryClient`：注册/心跳/查询请求组装处校验 `bizCode` 非空（`require(bizCode, "DDC biz-code is required")`），并透传进 serviceKey。
  - `DdcKeys`：`registryService/registryRevision` 的 key 增加 biz/app 两段：`join("registry","service", biz, app, env, ns, kind, digest)`；`registryInstance` 增加 biz/app；`registryCatalog/registryCatalogRevision` 增加 biz/app 参数（调用方同步）。
  - admin 侧：`DdcServiceRegistryRedisRepository` 的 key 组装同步（biz/app 从 serviceKey 取）；`DdcManagementFacade.serviceQuery()` 从 `DdcManagementServiceQuery` 增加 bizCode/appCode 字段并透传（query DTO 同步加字段）；`DdcRegistryAdminController` 查询参数加 `biz`/`appCode`（必填，沿用既有 env/ns 必填风格）。
  - Lua 脚本：**不改**（KEYS 数量与 ARGV 顺序不变，key 字符串内容变化由上层组装）。
- 兼容：老 SDK 数据在 Redis 中自然过期；老 SDK 请求（无 biz/app 的 canonical）解析失败被拒——可接受（契约版本上浮由用户控制）。

- [ ] **Step 1: 改 DdcServiceKey + 测试**

`DdcServiceKeyTest`（或既有 key 测试）增加：canonical 含 biz/app 首两行、parse 往返、ORDER 比较。

```java
@Test
void canonicalValueLeadsWithBizAndApp() {
    DdcServiceKey key = new DdcServiceKey(
            "pay-biz", "orders-app", "dev", "default",
            DdcServiceKind.HTTP_PROVIDER, "orders", "g1", "1.0.0", "http");
    String[] lines = key.canonicalValue().split("\n");
    assertThat(lines[0]).isEqualTo("pay-biz");
    assertThat(lines[1]).isEqualTo("orders-app");
    assertThat(DdcServiceKey.parse(key.canonicalValue())).isEqualTo(key);
}
```

- [ ] **Step 2: 运行确认失败** → `cd <starter> && mvn -q test -Dtest=*ServiceKey*`（编译失败）。
- [ ] **Step 3: 实现 starter 侧**（DdcServiceKey/DdcKeys/DTO/client/properties）
- [ ] **Step 4: 实现 admin 侧**（repository key 组装、facade query、controller 参数、DTO；按编译错误清单逐一更新）
- [ ] **Step 5: 两侧测试全量修复 + 通过**：`cd <starter> && mvn -q test`；`cd <admin> && mvn -q test`
- [ ] **Step 6: Commit** `feat(ddc): add biz and app dimensions to registry contract`

---

### Task 7: 禁用门控（注册/拉取校验四实体）

**Files:**
- Create: `<admin>/.../service/DdcScopeGate.java`
- Create: `<admin>/src/test/java/.../service/DdcScopeGateTest.java`
- Modify: `<admin>/.../service/DdcServiceRegistryService.java`（register 入口）、`.../service/DdcManagementFacade.java`（pull 入口）、`.../config/DdcErrorStatus.java`（+DDC_SCOPE_DISABLED，若 T2 未加）

**Interfaces:**
- Consumes: Task 6 的 serviceKey（含 biz/app）；四个 repository。
- Produces:
  - `@Component class DdcScopeGate`：
    - `void assertEnabled(String bizCode, String appCode, String env, String namespace)` —— 任一实体缺失或 `enabled=false` → `throw new CommonException(DdcErrorStatus.DDC_SCOPE_DISABLED)`；错误消息含明细 `"biz=bizCode app=appCode env=env ns=namespace disabled"`。
    - 5 秒本地缓存：`ConcurrentHashMap<String, CachedState>`（key=实体类型+code，value=Enabled/Disabled/NotFound + 过期时间）；命中缓存不查库；写路径（setEnabled/delete）调用 `invalidate(code)` 清缓存。
    - 查询实现：biz 表按 bizCode、app 表按 appCode（含 biz 校验归属一致）、env 表按 envCode、ns 表按 (appCode, namespace)。
  - `DdcServiceRegistryService.register`：构造 serviceKey 后先 `scopeGate.assertEnabled(...)`（从 registration.serviceKey 取 biz/app/env/ns），再走 Redis。
  - `DdcManagementFacade` 的配置拉取路径（`findConfig`/`pull` 等入口，实施时以 grep `pull(` 定位全部入口）加 `scopeGate.assertEnabled`。
- 心跳/续租路径不动。

- [ ] **Step 1: 写失败的 DdcScopeGateTest**（mock 四个 repository：全启用通过；任一禁用抛 `DDC_SCOPE_DISABLED`；缓存命中不重复查库——第二次调用时 verify repository 只调一次；invalidate 后重查）

```java
@Test
void rejectsWhenAppDisabled() {
    when(appRepository.findByAppCode("orders-app"))
            .thenReturn(Optional.of(entity("orders-app", false)));
    assertThatThrownBy(() -> gate.assertEnabled("pay-biz", "orders-app", "dev", "default"))
            .isInstanceOfSatisfying(CommonException.class,
                    e -> assertThat(e.getErrorStatus()).isEqualTo(DdcErrorStatus.DDC_SCOPE_DISABLED));
}

@Test
void cachesForFiveSeconds() {
    // 第一次调用后，第二次不触发 repository 查询（verifyNoMoreInteractions 或 times(1)）
}
```

- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现 DdcScopeGate + register/pull 接入**
- [ ] **Step 4: 运行测试确认通过** + admin 全量回归（既有 register/pull 测试需 mock gate 或补实体数据）
- [ ] **Step 5: Commit** `feat(ddc-admin): gate registration and pull on scope enabled state`

---

### Task 8: registry 查询响应带 appCode（前端聚合支撑）

**Files:**
- Modify: `<starter>/.../model/registry/DdcServiceSnapshot.java`（service 记录加 appCode/bizCode 字段）或 admin 侧 VO
- Modify: `<admin>/.../service/DdcManagementFacade.java`（catalog/instances 映射带 appCode）
- Modify: 相关测试

**Interfaces:**
- Consumes: Task 6 的契约（注册数据已含 biz/app）。
- Produces: `/api/v1/ddc/registry/services` 的 service 记录含 `appCode`、`bizCode` 字段；`/registry/instances` 的 instance 记录含 `appCode`（可从 serviceKey 或实例元数据带出，实施时以实际存储形态为准——若 serviceKey 存于 Redis 记录内，直接透传）。

- [ ] **Step 1: 改失败测试**（既有 registry controller 测试断言 service 记录带 appCode）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**（facade 映射处补充字段）
- [ ] **Step 4: 运行测试确认通过** + admin 全量
- [ ] **Step 5: Commit** `feat(ddc-admin): expose appCode in registry responses`

---

### Task 9: 前端 useScopeOptions 改造（biz→app→ns 级联 + env 独立）

**Files:**
- Modify: `<web>/src/components/scope/useScopeOptions.ts`、`useScopeOptions.test.ts`
- Modify: `<web>/src/api/types.ts`（+DdcBiz、DdcEnv 类型；DdcApp +bizCode；DdcNamespace -env）

**Interfaces:**
- Consumes: T2/T3/T5 端点。
- Produces:
  - 删除 `ENV_OPTIONS` 常量与 `/namespaces/domains` 逻辑。
  - `useScopeOptions(bizCode, appCode)` 返回 `{ bizs, apps, namespaces, envs, loading, reload }`：
    - bizs ← `GET /api/v1/ddc/bizs`（挂载一次）
    - apps ← `GET /api/v1/ddc/apps?biz={bizCode}`（biz 空不带参数）
    - namespaces ← `GET /api/v1/ddc/namespaces?appCode={appCode}`（app 空不带参数）
    - envs ← `GET /api/v1/ddc/envs`（挂载一次，按 sortOrder 升序）
    - 级联失效：biz 变 → 清 apps/namespaces；app 变 → 清 namespaces。
  - 缓存 key 沿用请求签名。

- [ ] **Step 1: 改失败测试**（级联链、envs 端点、无 ENV_OPTIONS）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**（按 Interfaces）
- [ ] **Step 4: 运行测试确认通过** + typecheck
- [ ] **Step 5: Commit** `feat(ddc-admin-web): cascade biz/app/ns options with backend envs`

---

### Task 10: 前端 ScopeSelects 四层改造（BizSelect 加入）

**Files:**
- Create: `<web>/src/components/scope/BizSelect.tsx`
- Modify: `<web>/src/components/scope/AppSelect.tsx`（+biz 参数）、`NamespaceSelect.tsx`（+app 参数）、`EnvSelect.tsx`（options 改后端）、`ScopeSelects.tsx`（四层 + 级联清空链）、`ScopeSelects.test.tsx`

**Interfaces:**
- Consumes: Task 9 的 hook。
- Produces:
  - `BizSelect({ value, onChange, disabled, placeholder='请选择或输入业务域' })` —— options=hook.bizs。
  - `AppSelect({ value, onChange, biz, ... })` —— options=hook(biz, '').apps。
  - `NamespaceSelect({ value, onChange, appCode, ... })` —— options=hook('', appCode).namespaces。
  - `EnvSelect({ value, onChange, ... })` —— options=hook('','').envs（后端数据）。
  - `ScopeSelects({ value: { bizCode, appCode, env, namespace }, onChange, includeApp, includeEnv, disabled })`：
    - 级联：biz 变 → 清 app+ns；app 变 → 清 ns；env 独立。
    - 宽度：biz 200 / app 200 / ns 200 / env 140。
  - `ScopeValue` 扩展：`{ bizCode: string; appCode: string; env: string; namespace: string }`。

- [ ] **Step 1: 改失败测试**（四层渲染、biz→app→ns 级联清空、envs 来自后端）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**（交互测试沿用"输入 + Enter"路径；宽度包装沿用既有 span 方案）
- [ ] **Step 4: 运行测试确认通过** + typecheck + lint
- [ ] **Step 5: Commit** `feat(ddc-admin-web): four-level scope selects with backend envs`

---

### Task 11: 业务域页 + 环境页（新管理页）

**Files:**
- Create: `<web>/src/pages/BizsPage.tsx`、`BizsPage.test.tsx`、`EnvPage.tsx`、`EnvPage.test.tsx`
- Modify: `<web>/src/App.tsx`（路由）、`src/layouts/AdminLayout.tsx`（菜单）

**Interfaces:**
- Consumes: T2/T3 端点。
- Produces: 两页与 AppsPage 同构（列表 + 模糊筛选 + 新建/编辑 Modal + 禁用 Switch/按钮 + 删除确认）：
  - BizsPage：列 = biz_code/biz_name/description/enabled/updatedAt；筛选 keyword 输入。
  - EnvPage：列 = env_code/description/sort_order/enabled/updatedAt；筛选 keyword；新建表单含 sort_order 数字输入。

- [ ] **Step 1: 写失败测试**（各 1 条：渲染 + 新建提交 body 断言，mock 按 URL 分发）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现两页 + 路由（`/bizs`、`/envs`）+ 菜单**
- [ ] **Step 4: 运行测试确认通过** + typecheck + lint
- [ ] **Step 5: Commit** `feat(ddc-admin-web): add biz and env management pages`

---

### Task 12: 应用页改造（biz 归属 + 筛选/编辑/禁用/删除）

**Files:**
- Modify: `<web>/src/pages/AppsPage.tsx`、`AppsPage.test.tsx`、`src/api/types.ts`（DdcApp +bizCode）

**Interfaces:**
- Consumes: T4 端点；T10 组件。
- Produces:
  - 筛选：BizSelect（必选后可查询）+ appCode/名称 keyword 输入 → `GET /apps?biz=&keyword=`。
  - 新建表单：bizCode → BizSelect（必填，不可输入新值也可输入——保持可输入）；appCode/appName/owner/description/enabled 不变。
  - 行操作：编辑（Modal 复用新建表单，biz 可改）、禁用/启用（Switch 列或按钮 → `PUT /apps/{code}/enabled`）、删除（confirm → `DELETE /apps/{code}`，后端 `DDC_APP_IN_USE` 错误 message 展示）。

- [ ] **Step 1: 改失败测试**（筛选带 biz、新建 body 含 bizCode、删除确认）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**
- [ ] **Step 4: 运行测试确认通过** + typecheck + lint
- [ ] **Step 5: Commit** `feat(ddc-admin-web): apps page with biz, filter, edit, disable, delete`

---

### Task 13: 命名空间页改造（无 env + 筛选/禁用/删除）

**Files:**
- Modify: `<web>/src/pages/NamespacesPage.tsx`、`NamespacesPage.test.tsx`、`src/api/types.ts`（DdcNamespace -env）

**Interfaces:**
- Consumes: T5 端点；T10 组件。
- Produces:
  - 筛选：AppSelect + ns keyword → `GET /namespaces?appCode=&keyword=`（无 env）。
  - 新建表单：appCode → AppSelect；namespace 输入；description；enabled；**无 env 字段**。
  - 行操作：编辑（ns 名可改）、禁用/启用、删除（confirm；`DDC_NAMESPACE_IN_USE` message 展示）。

- [ ] **Step 1: 改失败测试**（筛选 app+keyword、新建 body 无 env）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**
- [ ] **Step 4: 运行测试确认通过** + typecheck + lint
- [ ] **Step 5: Commit** `feat(ddc-admin-web): namespaces page without env with full actions`

---

### Task 14: 服务注册页重构（APP 聚合 + Drawer）+ 删除实例页

**Files:**
- Modify: `<web>/src/pages/RegistryPage.tsx`、`RegistryPage.test.tsx`、`src/api/types.ts`（RegistryService/RegistryInstance +appCode/bizCode）
- Delete: `<web>/src/pages/InstancesPage.tsx`
- Modify: `<web>/src/App.tsx`、`src/layouts/AdminLayout.tsx`（移除实例路由/菜单）

**Interfaces:**
- Consumes: T8 响应（service/instance 带 appCode）；T10 四层筛选。
- Produces:
  - 筛选：BizSelect → AppSelect → NamespaceSelect → EnvSelect（四层，注册查询参数 `biz & appCode & env & namespace & serviceKind & protocol`；后端 controller 参数在 T6 已加）。
  - 主视图：`GET /registry/services`（4 种 kind 并行 + 去重，沿用现状）→ 按 `appCode` 分组聚合：
    - 每 APP 一行：appCode、bizCode、env/ns（取该 app 记录首个）、在线实例数（点击展开时算或聚合响应中实例计数——实施简化：行内显示该 app 的服务数，在线实例数在抽屉内展示）。
    - 点击行 → Drawer：抽屉内按 serviceKey（kind/name/group/version）分组，组内实例表格（status 徽标/instanceId/host:port/lastHeartbeatAt/expireAt）；头部 = appCode + bizCode + 刷新按钮 + 分组计数。
  - 概览卡：APP 数、服务数、在线实例数（保留统计卡样式）。
  - 无实例 APP 不显示（有实例才聚合；如需全部 APP 后续加开关）。
  - 删除 `InstancesPage.tsx` 与路由 `/instances`、菜单项。

- [ ] **Step 1: 改失败测试**（聚合：mock services 响应两条同 appCode → 列表一行；点击行 → Drawer 出现实例分组）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**（Drawer 用 antd `Drawer`；分组用 Table `groupBy` 或组内小表格）
- [ ] **Step 4: 运行测试确认通过** + typecheck + lint + 全量
- [ ] **Step 5: Commit** `feat(ddc-admin-web): app-centric registry page with instance drawer, drop instances page`

---

### Task 15: 配置管理/缓存页四层筛选 + ensureScope biz 归属

**Files:**
- Modify: `<web>/src/pages/ConfigsPage.tsx`、`ConfigsPage.test.tsx`、`ConfigEditorDialog.tsx`、`CachePage.tsx`

**Interfaces:**
- Consumes: T10 组件。
- Produces:
  - ConfigsPage 筛选与对话框：ScopeSelects 四层（biz→app→ns→env）；config 查询参数 `appCode & env & namespace & configKey` 不变（biz 仅用于选择，不传查询）。
  - `ConfigEditorDialog` 的 `ensureAppAndNamespace`：自动创建 app 时 body 增加 `bizCode`（取当前选中 biz，缺失则 `message.warning('请先选择业务域')`）；自动创建 ns 时 body 去掉 env。
  - CachePage 筛选：四层 ScopeSelects；`check/rebuild` 参数 `appCode & env & namespace` 不变。
  - `ScopeValue` 引用处（ConfigFilter/InstanceFilter 等）同步扩展字段。

- [ ] **Step 1: 改失败测试**（配置新建对话框级联含 biz；mock 更新）
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**
- [ ] **Step 4: 运行测试确认通过** + typecheck + lint + 全量
- [ ] **Step 5: Commit** `feat(ddc-admin-web): four-level scope filters for configs and cache`

---

### Task 16: 全量验证 + 文档

**Files:**
- Modify: `<web>/README.md`、`README.zh-CN.md`、DDC 平台 README（模块说明段）

**Interfaces:**
- Consumes: Task 1-15 全部产物。

- [ ] **Step 1: 后端全量**：`cd <starter> && mvn -q clean test`；`cd <admin> && mvn -q clean test` —— BUILD SUCCESS。
- [ ] **Step 2: 前端全量**：`cd <web> && npm run typecheck && npm run lint && npm run test -- --run && npm run build` —— 全绿。
- [ ] **Step 3: 文档更新**：README 作用域下拉段落改为"业务域/应用/命名空间/环境四层 + 环境下拉来自后端实体"，并说明注册身份 biz-ns-env-app 与禁用门控。
- [ ] **Step 4: Commit** `docs(ddc): document scope model revision`

---

## 验收清单（对照 spec）

- [ ] 注册身份恒为 biz-ns-env-app（DdcServiceKey canonical/Redis key/查询参数全链路）。
- [ ] 任一实体禁用 → 新注册被拒（`DDC_SCOPE_DISABLED`）、配置拉取被拒；启用恢复；心跳不校验。
- [ ] biz/app/ns/env 四页可完整管理（增删改查 + 筛选 + 禁用），删除保护错误码正确。
- [ ] 新建命名空间不选环境；`/namespaces/domains` 与 `/apps?namespace=` 已移除。
- [ ] 配置管理按 (app, env, ns) 正常建配置；自动创建 app 归属所选 biz。
- [ ] 服务注册页按 APP 聚合展示、Drawer 按服务分组看实例；实例页与菜单项已移除。
- [ ] 环境下拉来自 `GET /envs`；前端无写死环境枚举。
- [ ] starter 与 admin 测试全绿；前端 typecheck/lint/vitest/build 全绿。

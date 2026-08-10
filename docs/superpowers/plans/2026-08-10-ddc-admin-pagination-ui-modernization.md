# DDC Admin Pagination and UI Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 DDC Admin 的 12 个集合型管理查询增加兼容的 `PageResultRecord` 服务端分页接口，并把 DDC Admin Web 全部管理表格迁移为稳定、响应式的 Ant Design 服务端分页界面。

**Architecture:** 原 List、Catalog、Snapshot 接口保持不变；数据库集合通过 Spring Data `Pageable` 和精确 `countQuery` 分页，Registry 完整快照通过 Admin 专用适配服务稳定切页。前端新增 `ddcPageApi` 和 React Query 基础设施，作用域选项继续使用原 List 接口，管理表格统一使用 `/page`。

**Tech Stack:** Java 21、Spring Boot MVC、Spring Data JPA、SQLite/PostgreSQL、Redisson、JUnit 5、MockMvc、React 19、TypeScript 6、Ant Design 6、React Query 5、Vitest、Testing Library、ESLint、Vite。

## Design Pattern Decision

- Registry 分页使用 **Adapter**：`DdcRegistryAdminPageService` 把既有 `DdcManagementFacade` 的完整 Catalog/Snapshot 适配为 Admin Web Page，不污染 Starter/RPC 的机器契约；这是已存在 Facade 之外唯一必要的新模式。
- 数据库集合直接使用 Repository `Page` + Service 编排，不增加 Strategy/Factory/Template Method；12 个查询的条件和返回类型不同，引入通用策略只会隐藏类型与 countQuery。
- 前端不增加万能 PagedTable Facade；只共享 Page API、QueryClient、page state 和 PageHeader，列、mutation、Drawer 与筛选保留在各页面内。

## Global Constraints

- 直接在当前 `main` 工作区执行，不创建分支或 worktree。
- 每个 Task 完成测试后单独提交一次，只暂存该 Task 明确列出的文件。
- 公共分页类型只能使用 `PageQuery`、`PageResultRecord<T>`、`PageMetaRecord`；不得新增 `ResultPageRecord`。
- 默认 `pageNo=1`、`pageSize=10`，后端最大 `pageSize=500`，前端可选 `10/20/50`。
- 新分页端点统一使用 `/page`；所有现有 List、Catalog、Snapshot URL 和成功响应结构保持不变。
- 数据库集合不得通过全量 List 加 `subList` 实现分页；只有 Registry Admin 聚合适配允许对已取得的完整快照稳定切页。
- 不修改 DDC Starter Java 端口、RPC Proto、RPC Provider、RPC DDC Adapter 或 `DdcManagementFacade` 公共签名。
- 不新增依赖，不修改 Common Core，不修改数据库结构，不修改已有 Flyway 文件，也不新增 Flyway 迁移。
- 前端必须使用现有 Ant Design、React Query 和 Admin Web Shared；不引入 Pro Components 或新的状态库。
- 前端不得继续新增 `window.confirm` 或静态 `message` 调用；使用 `App.useApp()`、`Popconfirm` 或 `Modal.confirm`。
- 不启动 DDC、Redis、PostgreSQL、Gateway、Vite dev server 或浏览器；验证限于 Maven、Vitest、typecheck、lint、build 和源码残留扫描。
- 测试和构建 DDC Admin Web 时统一提供非秘密环境变量：`VITE_IDP_ISSUER=http://127.0.0.1:18120`、`VITE_IDP_CLIENT_ID=ddc-admin-web`、`VITE_IDP_AUDIENCE=ddc-admin`。
- 保留用户工作区内任何无关改动；发现重叠修改时先审查，不回滚他人工作。

---

## File and Interface Map

### Backend new files

- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/support/DdcAdminPageSupport.java`：`PageQuery`、Spring `Pageable/Page` 与 `PageResultRecord` 的 Admin 边界适配。
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/dto/DdcPublishTaskQueryRequest.java`：发布任务分页筛选。
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/publish/DdcPublishTaskQueryService.java`：发布任务只读分页编排。
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/management/DdcRegistryAdminPageService.java`：完整 Registry Catalog/Snapshot 到 Admin Page 的适配。
- 对应的 support、service、repository 和 controller 测试文件。

### Backend modified areas

- Metadata Controllers、Services、Repositories 及 Controller/Repository 测试。
- `DdcConfigController`、`DdcConfigService`、Config Item/Version Repository 及测试。
- `DdcPublishTaskController`、`DdcInstanceController`、`DdcCacheController` 和对应 Service/Repository/测试。
- `DdcRegistryAdminController` 及其测试。

### Frontend new files

- `src/query/queryClient.ts`：唯一 QueryClient 工厂和生产实例。
- `src/hooks/usePageState.ts`：受控 `pageNo/pageSize` 状态。
- `src/components/page/AdminPageHeader.tsx`：统一标题、说明和主操作区域。
- `src/test/renderWithQueryClient.tsx`：每个测试独立 QueryClient 的渲染辅助。
- `src/styles/admin.css`：仅承载 Layout、响应式和表格溢出样式。

### Frontend modified areas

- `src/api/types.ts`、`src/api/client.ts`、`src/api/client.test.ts`。
- `src/main.tsx`、`src/components/scope/useScopeOptions.ts`、`src/components/scope/useScopeOptions.test.ts`、`src/components/scope/ScopeSelects.tsx`、`src/components/scope/ScopeSelects.test.tsx`。
- 8 个页面、其测试、`AdminLayout.tsx`、`App.tsx`。
- Admin Web 英文和中文 README。

### Interfaces produced in order

```java
public final class DdcAdminPageSupport {
    public static Pageable pageable(PageQuery query, Sort sort);
    public static Pageable pageable(PageQuery query);
    public static <T> Page<T> slice(List<T> records, PageQuery query);
    public static <T> PageResultRecord<T> result(Page<T> page);
}
```

```ts
export type PageMetaRecord = {
  total: number
  pageNo: number
  pageSize: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

export type PageResultRecord<T> = ResultEnvelope & {
  records: T[]
  page: PageMetaRecord
}

export async function ddcPageApi<T>(
  path: string,
  options?: DdcRequestOptions,
): Promise<PageResultRecord<T>>
```

---

### Task 1: 分页 Support 与 Biz/Namespace 基础分页

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/support/DdcAdminPageSupport.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/support/DdcAdminPageSupportTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcBizRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcBizService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcNamespaceService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcBizController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcNamespaceController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcBizControllerTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceControllerTest.java`

**Interfaces:**
- Consumes: Common Core `PageQuery` and `PageResultRecord<T>`; existing Biz/Namespace List APIs.
- Produces: `DdcAdminPageSupport`; `DdcBizService.page(String, PageQuery)`; `DdcNamespaceService.page(String, String, PageQuery)`; `/bizs/page`; `/namespaces/page`.

- [ ] **Step 1: 为 Page Support 写失败测试**

```java
@Test
void normalizesPageRequestAndBuildsPublicResult() {
    Pageable pageable = DdcAdminPageSupport.pageable(
            new PageQuery(2, 20),
            Sort.by("bizCode").ascending()
    );
    Page<String> page = new PageImpl<>(
            List.of("pay"), pageable, 21
    );

    PageResultRecord<String> result = DdcAdminPageSupport.result(page);

    assertThat(pageable.getPageNumber()).isEqualTo(1);
    assertThat(result.records()).containsExactly("pay");
    assertThat(result.page().total()).isEqualTo(21);
    assertThat(result.page().pageNo()).isEqualTo(2);
    assertThat(result.page().pageSize()).isEqualTo(20);
}

@Test
void slicesOnlyAggregateRecords() {
    Page<String> page = DdcAdminPageSupport.slice(
            List.of("a", "b", "c"), new PageQuery(2, 2)
    );
    assertThat(page.getContent()).containsExactly("c");
    assertThat(page.getTotalElements()).isEqualTo(3);
}
```

- [ ] **Step 2: 运行 Support 测试并确认失败**

Run:

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcAdminPageSupportTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，提示 `DdcAdminPageSupport` 不存在。

- [ ] **Step 3: 实现最小 Page Support**

```java
public final class DdcAdminPageSupport {

    private DdcAdminPageSupport() {
    }

    public static Pageable pageable(PageQuery query, Sort sort) {
        PageQuery value = query == null ? PageQuery.defaultPage() : query;
        return PageRequest.of(value.pageNo() - 1, value.pageSize(), sort);
    }

    public static Pageable pageable(PageQuery query) {
        return pageable(query, Sort.unsorted());
    }

    public static <T> Page<T> slice(List<T> records, PageQuery query) {
        List<T> values = records == null ? List.of() : List.copyOf(records);
        Pageable pageable = pageable(query);
        int from = (int) Math.min(pageable.getOffset(), values.size());
        int to = Math.min(from + pageable.getPageSize(), values.size());
        return new PageImpl<>(values.subList(from, to), pageable, values.size());
    }

    public static <T> PageResultRecord<T> result(Page<T> page) {
        return PageResultRecord.success(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize()
        );
    }
}
```

- [ ] **Step 4: 为 Biz 和 Namespace Controller 写失败契约测试**

```java
@Test
void pagesBizsWithoutChangingLegacyList() throws Exception {
    DdcBizEntity biz = new DdcBizEntity();
    biz.setId("biz-1");
    biz.setBizCode("pay");
    biz.setBizName("支付");
    when(bizService.page(eq("pay"), any(PageQuery.class)))
            .thenReturn(new PageImpl<>(
                    List.of(biz), PageRequest.of(1, 20), 21
            ));

    mockMvc.perform(get("/api/v1/ddc/bizs/page")
                    .param("keyword", "pay")
                    .param("pageNo", "2")
                    .param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[0].bizCode").value("pay"))
            .andExpect(jsonPath("$.page.total").value(21))
            .andExpect(jsonPath("$.page.pageNo").value(2))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

Namespace 测试增加完整请求和 Page envelope 断言：

```java
when(namespaceService.page(eq("infra"), eq("ops"), any(PageQuery.class)))
        .thenReturn(new PageImpl<>(List.of(namespace), PageRequest.of(0, 10), 1));

mockMvc.perform(get("/api/v1/ddc/namespaces/page")
                .param("bizCode", "infra")
                .param("keyword", "ops")
                .param("pageNo", "1")
                .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.records[0].namespaceCode").value("ops"))
        .andExpect(jsonPath("$.page.total").value(1))
        .andExpect(jsonPath("$.page.pageNo").value(1))
        .andExpect(jsonPath("$.data").doesNotExist());
```

并继续保留现有 List 测试对 `$.data` 的断言。

- [ ] **Step 5: 运行 Controller 测试并确认 `/page` 失败**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcBizControllerTest,DdcNamespaceControllerTest,DdcAdminPageSupportTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，`/page` 为 404 或 Service 方法不存在。

- [ ] **Step 6: 增加 Biz/Namespace Repository Page 查询**

```java
@Query("""
        select biz from DdcBizEntity biz
         where (:keyword is null
                or lower(biz.bizCode) like lower(concat('%', :keyword, '%'))
                or lower(biz.bizName) like lower(concat('%', :keyword, '%')))
        """)
Page<DdcBizEntity> search(
        @Param("keyword") String keyword,
        Pageable pageable
);
```

```java
@Query("""
        select namespace from DdcNamespaceEntity namespace
         where (:bizCode is null or namespace.bizCode = :bizCode)
           and (:keyword is null
                or lower(namespace.namespaceCode) like lower(concat('%', :keyword, '%'))
                or lower(namespace.namespace) like lower(concat('%', :keyword, '%')))
        """)
Page<DdcNamespaceEntity> search(
        @Param("bizCode") String bizCode,
        @Param("keyword") String keyword,
        Pageable pageable
);
```

- [ ] **Step 7: 增加 Service Page 方法与 Controller `/page`**

```java
public Page<DdcBizEntity> page(String keyword, PageQuery pageQuery) {
    return bizRepository.search(
            optional(keyword),
            DdcAdminPageSupport.pageable(
                    pageQuery,
                    Sort.by("bizCode").ascending().and(Sort.by("id").ascending())
            )
    );
}
```

```java
@GetMapping("/page")
public PageResultRecord<DdcBizEntity> page(
        @RequestParam(value = "keyword", required = false) String keyword,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(bizService.page(keyword, pageQuery));
}
```

Namespace 明确增加以下 Service 和 Controller 方法；在两个 Service 中增加私有 `optional(String)`，将空白归一化为 `null`，不改变原 `list(...)`：

```java
public Page<DdcNamespaceEntity> page(
        String bizCode, String keyword, PageQuery pageQuery) {
    return namespaceRepository.search(
            optional(bizCode),
            optional(keyword),
            DdcAdminPageSupport.pageable(
                    pageQuery,
                    Sort.by("bizCode").ascending()
                            .and(Sort.by("namespaceCode").ascending())
                            .and(Sort.by("id").ascending())
            )
    );
}
```

```java
@GetMapping("/page")
public PageResultRecord<DdcNamespaceEntity> page(
        @RequestParam(value = "bizCode", required = false) String bizCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(
            namespaceService.page(bizCode, keyword, pageQuery));
}
```

- [ ] **Step 8: 运行 Task 1 测试并确认通过**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcAdminPageSupportTest,DdcBizControllerTest,DdcNamespaceControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，0 failures。

- [ ] **Step 9: 提交 Task 1**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/support/DdcAdminPageSupport.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcBizRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcBizService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcNamespaceService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcBizController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcNamespaceController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/support/DdcAdminPageSupportTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcBizControllerTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceControllerTest.java
git commit -m "feat(ddc): add core metadata page queries"
```

---

### Task 2: Env/App 可见性数据库分页

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcEnvRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcAppRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcEnvService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcAppService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcEnvController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcAppController.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcMetadataPagingRepositoryTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcEnvControllerTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcAppControllerTest.java`

**Interfaces:**
- Consumes: `DdcAdminPageSupport` from Task 1 and current namespace/env/app visibility semantics.
- Produces: `DdcEnvService.page(...)`、`DdcAppService.page(...)`、`/envs/page`、`/apps/page`，可见性和 keyword 在数据库内完成。

- [ ] **Step 1: 写 Env/App Repository 失败测试**

在 SQLite `@DataJpaTest` 中创建两个 biz、两个 namespace、三个 app、两个 env 和绑定，断言：

```java
Page<DdcEnvEntity> envPage = envRepository.search(
        "infra", "default", "pro",
        PageRequest.of(0, 10, Sort.by("sortOrder", "envCode", "id"))
);
assertThat(envPage.getContent())
        .extracting(DdcEnvEntity::getEnvCode)
        .containsExactly("prod");
assertThat(envPage.getTotalElements()).isEqualTo(1);

Page<DdcAppEntity> appPage = appRepository.search(
        "infra", "default", "prod", "gate",
        PageRequest.of(0, 10, Sort.by("bizCode", "appCode", "id"))
);
assertThat(appPage.getContent())
        .extracting(DdcAppEntity::getAppCode)
        .containsExactly("gateway");
assertThat(appPage.getTotalElements()).isEqualTo(1);
```

- [ ] **Step 2: 运行 Repository 测试并确认失败**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcMetadataPagingRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，`search(...)` Page 方法不存在。

- [ ] **Step 3: 实现 Env 的 JPQL 可见性 Page 查询**

```java
@Query("""
        select env from DdcEnvEntity env
         where (:keyword is null
                or lower(env.envCode) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(env.description, '')) like lower(concat('%', :keyword, '%')))
           and (:bizCode is null or :namespaceCode is null or exists (
                select binding.id
                  from DdcNamespaceEnvAppBindingEntity binding,
                       DdcNamespaceEntity namespace
                 where binding.namespaceId = namespace.id
                   and binding.envCode = env.envCode
                   and binding.enabled = true
                   and namespace.enabled = true
                   and namespace.bizCode = :bizCode
                   and namespace.namespaceCode = :namespaceCode))
        """)
Page<DdcEnvEntity> search(
        @Param("bizCode") String bizCode,
        @Param("namespaceCode") String namespaceCode,
        @Param("keyword") String keyword,
        Pageable pageable
);
```

- [ ] **Step 4: 实现 App 的 JPQL 可见性 Page 查询**

```java
@Query("""
        select app from DdcAppEntity app
         where (:bizCode is null or app.bizCode = :bizCode)
           and (:keyword is null
                or lower(app.appCode) like lower(concat('%', :keyword, '%'))
                or lower(app.appName) like lower(concat('%', :keyword, '%')))
           and (:namespaceCode is null or :env is null or exists (
                select binding.id
                  from DdcNamespaceEnvAppBindingEntity binding,
                       DdcNamespaceEntity namespace
                 where binding.namespaceId = namespace.id
                   and binding.appId = app.id
                   and binding.envCode = :env
                   and binding.enabled = true
                   and namespace.enabled = true
                   and namespace.bizCode = app.bizCode
                   and namespace.namespaceCode = :namespaceCode))
        """)
Page<DdcAppEntity> search(
        @Param("bizCode") String bizCode,
        @Param("namespaceCode") String namespaceCode,
        @Param("env") String env,
        @Param("keyword") String keyword,
        Pageable pageable
);
```

`DdcAppService.page(...)` 必须保留当前特殊语义：同时给出 `namespaceCode + env` 却缺失 `bizCode` 时返回 `Page.empty(pageable)`。

- [ ] **Step 5: 写并运行 Env/App Controller 失败测试**

Env 断言 `/envs/page?bizCode=infra&namespaceCode=default&pageNo=1&pageSize=10` 返回 `$.records`；App 断言所有 scope + keyword 被传给 Service，并验证 `$.page.total`。

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcEnvControllerTest,DdcAppControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，Page Service/Controller 尚不存在。

- [ ] **Step 6: 实现 Service 和 Controller**

```java
public Page<DdcAppEntity> page(
        String bizCode,
        String namespaceCode,
        String env,
        String keyword,
        PageQuery pageQuery) {
    Pageable pageable = DdcAdminPageSupport.pageable(
            pageQuery,
            Sort.by("bizCode").ascending()
                    .and(Sort.by("appCode").ascending())
                    .and(Sort.by("id").ascending())
    );
    if (hasText(namespaceCode) && hasText(env) && !hasText(bizCode)) {
        return Page.empty(pageable);
    }
    return appRepository.search(
            optional(bizCode), optional(namespaceCode),
            optional(env), optional(keyword), pageable
    );
}
```

Env Service 和两个 Controller 方法固定为：

```java
public Page<DdcEnvEntity> page(
        String bizCode,
        String namespaceCode,
        String keyword,
        PageQuery pageQuery) {
    return envRepository.search(
            optional(bizCode), optional(namespaceCode), optional(keyword),
            DdcAdminPageSupport.pageable(
                    pageQuery,
                    Sort.by("sortOrder").ascending()
                            .and(Sort.by("envCode").ascending())
                            .and(Sort.by("id").ascending())
            )
    );
}

@GetMapping("/page")
public PageResultRecord<DdcEnvEntity> page(
        @RequestParam(value = "bizCode", required = false) String bizCode,
        @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(
            envService.page(bizCode, namespaceCode, keyword, pageQuery));
}

@GetMapping("/page")
public PageResultRecord<DdcAppEntity> page(
        @RequestParam(value = "bizCode", required = false) String bizCode,
        @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
        @RequestParam(value = "env", required = false) String env,
        @RequestParam(value = "keyword", required = false) String keyword,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(appService.page(
            bizCode, namespaceCode, env, keyword, pageQuery));
}
```

不删除或改写原 `list(...)`。

- [ ] **Step 7: 运行 Task 2 测试**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcMetadataPagingRepositoryTest,DdcEnvControllerTest,DdcAppControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，0 failures。

- [ ] **Step 8: 提交 Task 2**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcEnvRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcAppRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcEnvService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcAppService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcEnvController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcAppController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcMetadataPagingRepositoryTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcEnvControllerTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcAppControllerTest.java
git commit -m "feat(ddc): paginate scope-aware metadata"
```

---

### Task 3: Namespace Binding Join 投影分页

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceEnvAppBindingRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcNamespaceEnvAppBindingService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcNamespaceEnvAppBindingController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcMetadataPagingRepositoryTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/metadata/DdcNamespaceEnvAppBindingServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceEnvAppBindingControllerTest.java`

**Interfaces:**
- Consumes: `DdcNamespaceEnvAppBindingVO` 构造器、`DdcAdminPageSupport`。
- Produces: 单 Join `Page<DdcNamespaceEnvAppBindingVO>`；`bindingService.page(...)`；`/namespace-env-app-bindings/page`。

- [ ] **Step 1: 写 Join Page 失败测试**

在 `DdcMetadataPagingRepositoryTest` 复用已保存的 namespace/app/binding 数据：

```java
Page<DdcNamespaceEnvAppBindingVO> page = bindingRepository.search(
        "infra", "default", "prod", "gateway",
        PageRequest.of(0, 10)
);

assertThat(page.getTotalElements()).isEqualTo(1);
assertThat(page.getContent()).singleElement().satisfies(row -> {
    assertThat(row.bizCode()).isEqualTo("infra");
    assertThat(row.namespaceCode()).isEqualTo("default");
    assertThat(row.env()).isEqualTo("prod");
    assertThat(row.appCode()).isEqualTo("gateway");
});
```

- [ ] **Step 2: 运行测试并确认失败**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcMetadataPagingRepositoryTest,DdcNamespaceEnvAppBindingServiceTest,DdcNamespaceEnvAppBindingControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，Repository Page 方法不存在。

- [ ] **Step 3: 增加 JPQL Constructor Projection**

```java
@Query(value = """
        select new top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO(
               binding.id,
               namespace.bizCode,
               namespace.id,
               namespace.namespaceCode,
               binding.envCode,
               app.id,
               app.appCode,
               app.appName,
               binding.enabled)
          from DdcNamespaceEnvAppBindingEntity binding
          join DdcNamespaceEntity namespace on namespace.id = binding.namespaceId
          join DdcAppEntity app on app.id = binding.appId
         where (:bizCode is null or namespace.bizCode = :bizCode)
           and (:namespaceCode is null or namespace.namespaceCode = :namespaceCode)
           and (:env is null or binding.envCode = :env)
           and (:appCode is null or app.appCode = :appCode)
         order by namespace.bizCode, namespace.namespaceCode,
                  binding.envCode, app.appCode, binding.id
        """,
        countQuery = """
        select count(binding)
          from DdcNamespaceEnvAppBindingEntity binding
          join DdcNamespaceEntity namespace on namespace.id = binding.namespaceId
          join DdcAppEntity app on app.id = binding.appId
         where (:bizCode is null or namespace.bizCode = :bizCode)
           and (:namespaceCode is null or namespace.namespaceCode = :namespaceCode)
           and (:env is null or binding.envCode = :env)
           and (:appCode is null or app.appCode = :appCode)
        """)
Page<DdcNamespaceEnvAppBindingVO> search(
        @Param("bizCode") String bizCode,
        @Param("namespaceCode") String namespaceCode,
        @Param("env") String env,
        @Param("appCode") String appCode,
        Pageable pageable
);
```

- [ ] **Step 4: 增加 Service Page 与 Controller Page**

```java
public Page<DdcNamespaceEnvAppBindingVO> page(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode,
        PageQuery pageQuery) {
    return bindingRepository.search(
            optional(bizCode), optional(namespaceCode),
            optional(env), optional(appCode),
            DdcAdminPageSupport.pageable(pageQuery)
    );
}
```

Controller 增加以下方法。原 `list(...)` 和 Service 内 `toVO(...)` 继续为完整 List、create/update 使用：

```java
@GetMapping("/page")
public PageResultRecord<DdcNamespaceEnvAppBindingVO> page(
        @RequestParam(value = "bizCode", required = false) String bizCode,
        @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
        @RequestParam(value = "env", required = false) String env,
        @RequestParam(value = "appCode", required = false) String appCode,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(bindingService.page(
            bizCode, namespaceCode, env, appCode, pageQuery));
}
```

- [ ] **Step 5: 增加防回归断言**

Service 测试用 Mockito 验证 Page 路径只调用一次 `bindingRepository.search(...)`，并执行：

```java
verify(namespaceRepository, never()).findById(anyString());
verify(appRepository, never()).findById(anyString());
```

Controller 测试断言 `$.records[0].appCode`、`$.page.total`，原 List 测试仍断言 `$.data[0].appCode`。

- [ ] **Step 6: 运行 Task 3 测试**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcMetadataPagingRepositoryTest,DdcNamespaceEnvAppBindingServiceTest,DdcNamespaceEnvAppBindingControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，0 failures。

- [ ] **Step 7: 提交 Task 3**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceEnvAppBindingRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcNamespaceEnvAppBindingService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/metadata/DdcNamespaceEnvAppBindingController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcMetadataPagingRepositoryTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/metadata/DdcNamespaceEnvAppBindingServiceTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceEnvAppBindingControllerTest.java
git commit -m "perf(ddc): paginate namespace bindings"
```

---

### Task 4: Config 与 Version 真实分页

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigItemRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigVersionRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/config/DdcConfigService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/config/DdcConfigController.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcConfigPagingRepositoryTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/config/DdcConfigServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcConfigControllerTest.java`

**Interfaces:**
- Consumes: 原 Config native search、`DdcConfigQueryRequest`、`DdcAdminPageSupport`。
- Produces: `configService.page(...)`、`configService.pageVersions(...)`、`/configs/page`、`/configs/{id}/versions/page`。

- [ ] **Step 1: 写 Config content/count 失败测试**

创建 12 条配置，其中 11 条匹配 `bizCode=infra` 且未删除，一条属于其他 biz：

```java
Page<DdcConfigItemEntity> page = configItemRepository.search(
        "infra", null, null, null, null, false,
        PageRequest.of(1, 5)
);

assertThat(page.getContent()).hasSize(5);
assertThat(page.getTotalElements()).isEqualTo(11);
assertThat(page.getNumber()).isEqualTo(1);
```

创建三个版本并断言：

```java
Page<DdcConfigVersionEntity> versions = versionRepository
        .findByConfigIdOrderByVersionDescIdDesc(
                "config-1", PageRequest.of(0, 2)
        );
assertThat(versions.getContent())
        .extracting(DdcConfigVersionEntity::getVersion)
        .containsExactly(3L, 2L);
assertThat(versions.getTotalElements()).isEqualTo(3);
```

- [ ] **Step 2: 运行 Repository 测试并确认失败**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcConfigPagingRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，Page overload 和 Version Page 方法不存在。

- [ ] **Step 3: 为 native search 增加 Page overload 和 countQuery**

保留当前 List `search(...)`。新增同名 Page overload，查询 SQL 的 where 条件逐字对齐现有语义，并固定：

```sql
order by c.biz_code, c.env, c.app_code, c.config_key, c.id
```

对应 countQuery：

```sql
select count(*)
  from ddc_config_item c
 where (:bizCode is null or c.biz_code = :bizCode)
   and (:env is null or c.env = :env)
   and (:appCode is null or c.app_code = :appCode)
   and (:resourceName is null or c.config_key like ('%' || :resourceName || '%'))
   and (:includeDeleted = true or c.deleted = false)
   and (:namespaceCode is null or exists (
       select 1
         from ddc_namespace_env_app b
         join ddc_namespace n on n.id = b.namespace_id
         join ddc_app a on a.id = b.app_id
        where b.enabled = true
          and n.enabled = true
          and n.namespace_code = :namespaceCode
          and n.biz_code = c.biz_code
          and a.biz_code = c.biz_code
          and a.app_code = c.app_code
          and b.env_code = c.env))
```

方法尾部参数为 `Pageable pageable`，返回 `Page<DdcConfigItemEntity>`。

- [ ] **Step 4: 增加 Version Page Repository**

```java
Page<DdcConfigVersionEntity> findByConfigIdOrderByVersionDescIdDesc(
        String configId,
        Pageable pageable
);
```

- [ ] **Step 5: 写 Config Controller 失败契约测试**

```java
when(configService.page(any(DdcConfigQueryRequest.class), any(PageQuery.class)))
        .thenReturn(new PageImpl<>(
                List.of(config), PageRequest.of(0, 10), 12
        ));

mockMvc.perform(get("/api/v1/ddc/configs/page")
                .param("bizCode", "infra")
                .param("pageNo", "1")
                .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.records[0].id").value(config.getId()))
        .andExpect(jsonPath("$.page.total").value(12));
```

Version 路由增加独立契约断言：

```java
when(configService.pageVersions(eq("config-1"), any(PageQuery.class)))
        .thenReturn(new PageImpl<>(
                List.of(version), PageRequest.of(1, 20), 21
        ));

mockMvc.perform(get("/api/v1/ddc/configs/config-1/versions/page")
                .param("pageNo", "2")
                .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.records[0].id").value(version.getId()))
        .andExpect(jsonPath("$.page.total").value(21))
        .andExpect(jsonPath("$.page.pageNo").value(2));
```

- [ ] **Step 6: 实现 Config Service Page 和 Controller Page**

```java
public Page<DdcConfigVO> page(
        DdcConfigQueryRequest request,
        PageQuery pageQuery) {
    DdcConfigQueryRequest query = request == null
            ? new DdcConfigQueryRequest()
            : request;
    return configItemRepository.search(
            optional(query.getBizCode()),
            optional(query.getNamespaceCode()),
            optional(query.getEnv()),
            optional(query.getAppCode()),
            null,
            query.isIncludeDeleted(),
            DdcAdminPageSupport.pageable(pageQuery)
    ).map(this::config);
}

public Page<DdcConfigVersionVO> pageVersions(
        String configId,
        PageQuery pageQuery) {
    return versionRepository.findByConfigIdOrderByVersionDescIdDesc(
            configId,
            DdcAdminPageSupport.pageable(pageQuery)
    ).map(DdcConfigVersionVO::from);
}
```

Controller 增加两个确定的 Page 方法，原 `list` 和 `versions` 不变：

```java
@GetMapping("/page")
public PageResultRecord<DdcConfigVO> page(
        DdcConfigQueryRequest request,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(configService.page(request, pageQuery));
}

@GetMapping("/{id}/versions/page")
public PageResultRecord<DdcConfigVersionVO> pageVersions(
        @PathVariable("id") String id,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(
            configService.pageVersions(id, pageQuery));
}
```

- [ ] **Step 7: 运行 Task 4 测试**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcConfigPagingRepositoryTest,DdcConfigServiceTest,DdcConfigControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，0 failures。

- [ ] **Step 8: 提交 Task 4**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigItemRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigVersionRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/config/DdcConfigService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/config/DdcConfigController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcConfigPagingRepositoryTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/config/DdcConfigServiceTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcConfigControllerTest.java
git commit -m "feat(ddc): paginate config history"
```

---

### Task 5: Publish Task、持久化 Instance 与 Cache Check 分页

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/dto/DdcPublishTaskQueryRequest.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/publish/DdcPublishTaskQueryService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcPublishTaskRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/config/DdcPublishTaskController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcInstanceRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcInstanceAdminService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/register/DdcInstanceController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigVersionRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/cache/DdcCacheService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/config/DdcCacheController.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcOperationalPagingRepositoryTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/publish/DdcPublishTaskQueryServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/cache/DdcCacheServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/lease/DdcInstanceAdminServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcPublishTaskControllerTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcInstanceControllerTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcCacheControllerTest.java`

**Interfaces:**
- Consumes: `DdcAdminPageSupport`、现有 Publish Detail/Retry、Instance List、Cache Check。
- Produces: `/publish-tasks/page`、`/instances/page`、`/cache/check/page` 及对应 Service Page。

- [ ] **Step 1: 写 Operational Repository 失败测试**

发布任务：

```java
Page<DdcPublishTaskEntity> tasks = publishTaskRepository.search(
        "infra", "prod", "gateway", "FAILED", "019",
        PageRequest.of(0, 10)
);
assertThat(tasks.getContent()).extracting(DdcPublishTaskEntity::getStatus)
        .containsOnly("FAILED");
```

持久化实例：

```java
Page<DdcInstanceEntity> instances = instanceRepository
        .findByBizCodeAndEnvAndAppCode(
                "infra", "prod", "gateway",
                PageRequest.of(0, 10,
                        Sort.by(Sort.Direction.DESC, "updatedAt", "id"))
        );
assertThat(instances.getTotalElements()).isEqualTo(2);
```

Cache seed：

```java
Page<DdcConfigVersionEntity> versions = versionRepository
        .findPublishedRuntimeVersions(
                "infra", "prod", "gateway", "DELETE",
                PageRequest.of(0, 1)
        );
assertThat(versions.getTotalElements()).isEqualTo(2);
assertThat(versions.getContent()).hasSize(1);
```

- [ ] **Step 2: 运行失败测试**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcOperationalPagingRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，三个 Page Repository 方法不存在。

- [ ] **Step 3: 实现 Publish Task query DTO、Repository 和 Service**

```java
@Getter
@Setter
public class DdcPublishTaskQueryRequest {
    private String bizCode;
    private String env;
    private String appCode;
    private String status;
    private String changeId;
}
```

```java
@Query("""
        select task from DdcPublishTaskEntity task
         where (:bizCode is null or task.bizCode = :bizCode)
           and (:env is null or task.env = :env)
           and (:appCode is null or task.appCode = :appCode)
           and (:status is null or task.status = :status)
           and (:changeId is null
                or lower(task.changeId) like lower(concat('%', :changeId, '%')))
        """)
Page<DdcPublishTaskEntity> search(
        String bizCode,
        String env,
        String appCode,
        String status,
        String changeId,
        Pageable pageable
);
```

`DdcPublishTaskQueryService.page(request, pageQuery)` 归一化空白，并使用 `createdAt DESC, id DESC`：

```java
public Page<DdcPublishTaskEntity> page(
        DdcPublishTaskQueryRequest request,
        PageQuery pageQuery) {
    DdcPublishTaskQueryRequest query = request == null
            ? new DdcPublishTaskQueryRequest()
            : request;
    return publishTaskRepository.search(
            optional(query.getBizCode()),
            optional(query.getEnv()),
            optional(query.getAppCode()),
            optional(query.getStatus()),
            optional(query.getChangeId()),
            DdcAdminPageSupport.pageable(
                    pageQuery,
                    Sort.by(Sort.Direction.DESC, "createdAt", "id")
            )
    );
}
```

Controller 注入该 Service 并新增以下方法；原 Detail/Retry 依赖保持不变：

```java
@GetMapping("/page")
public PageResultRecord<DdcPublishTaskEntity> page(
        DdcPublishTaskQueryRequest request,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(
            publishTaskQueryService.page(request, pageQuery));
}
```

- [ ] **Step 4: 实现 Instance Page**

Repository 增加：

```java
Page<DdcInstanceEntity> findByBizCodeAndEnvAndAppCode(
        String bizCode,
        String env,
        String appCode,
        Pageable pageable
);
```

Service 增加：

```java
public Page<DdcInstanceEntity> page(
        String bizCode,
        String env,
        String appCode,
        PageQuery pageQuery) {
    return instanceRepository.findByBizCodeAndEnvAndAppCode(
            bizCode,
            env,
            appCode,
            DdcAdminPageSupport.pageable(
                    pageQuery,
                    Sort.by(Sort.Direction.DESC, "updatedAt", "id")
            )
    );
}
```

Controller 新增 `/page`，三个 scope 参数仍为必填：

```java
@GetMapping("/page")
public PageResultRecord<DdcInstanceEntity> page(
        @RequestParam("bizCode") String bizCode,
        @RequestParam("env") String env,
        @RequestParam("appCode") String appCode,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(instanceAdminService.page(
            bizCode, env, appCode, pageQuery));
}
```

- [ ] **Step 5: 实现 Cache seed Page 和当前页检查**

Version Repository 增加：

```java
@Query("""
        select version
          from DdcConfigVersionEntity version,
               DdcConfigItemEntity item
         where version.configId = item.id
           and version.version = item.publishedVersion
           and item.bizCode = :bizCode
           and item.env = :env
           and item.appCode = :appCode
           and item.deleted = false
           and (version.changeType is null or version.changeType <> :deleteType)
        """)
Page<DdcConfigVersionEntity> findPublishedRuntimeVersions(
        String bizCode,
        String env,
        String appCode,
        String deleteType,
        Pageable pageable
);
```

Cache Service 增加：

```java
public Page<DdcCacheCheckRow> page(
        String bizCode,
        String env,
        String appCode,
        PageQuery pageQuery) {
    return versionRepository.findPublishedRuntimeVersions(
            bizCode,
            env,
            appCode,
            ChangeType.DELETE.name(),
            DdcAdminPageSupport.pageable(
                    pageQuery,
                    Sort.by("resourceName").ascending()
                            .and(Sort.by("id").ascending())
            )
    ).map(version -> checkVersion(bizCode, env, appCode, version));
}
```

Controller 新增 `/check/page`。原 `/check` 和 `/rebuild` 不变：

```java
@GetMapping("/check/page")
public PageResultRecord<DdcCacheCheckRow> page(
        @RequestParam("bizCode") String bizCode,
        @RequestParam("env") String env,
        @RequestParam("appCode") String appCode,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(
            cacheService.page(bizCode, env, appCode, pageQuery));
}
```

- [ ] **Step 6: 增加三个 Controller 契约测试**

每个测试使用 `PageImpl`，统一断言：

```java
.andExpect(jsonPath("$.records").isArray())
.andExpect(jsonPath("$.page.total").value(expectedTotal))
.andExpect(jsonPath("$.page.pageNo").value(1))
.andExpect(jsonPath("$.data").doesNotExist());
```

Cache Service 测试额外 verify 只为当前页的 version 调用 Redis Repository。

- [ ] **Step 7: 运行 Task 5 测试**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcOperationalPagingRepositoryTest,DdcPublishTaskQueryServiceTest,DdcCacheServiceTest,DdcInstanceAdminServiceTest,DdcPublishTaskControllerTest,DdcInstanceControllerTest,DdcCacheControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，0 failures。

- [ ] **Step 8: 提交 Task 5**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/dto/DdcPublishTaskQueryRequest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/publish/DdcPublishTaskQueryService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcPublishTaskRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/config/DdcPublishTaskController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcInstanceRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcInstanceAdminService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/register/DdcInstanceController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigVersionRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/cache/DdcCacheService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/config/DdcCacheController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcOperationalPagingRepositoryTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/publish/DdcPublishTaskQueryServiceTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/cache/DdcCacheServiceTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/lease/DdcInstanceAdminServiceTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcPublishTaskControllerTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcInstanceControllerTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcCacheControllerTest.java
git commit -m "feat(ddc): paginate admin operational queries"
```

---

### Task 6: Registry Admin Page Adapter

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/management/DdcRegistryAdminPageService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/register/DdcRegistryAdminController.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/management/DdcRegistryAdminPageServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryAdminControllerTest.java`
- Verify unchanged: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/api/client/DdcManagementClient.java`
- Verify unchanged: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/rpc/provider/DdcManagementRpcProvider.java`

**Interfaces:**
- Consumes: `DdcManagementFacade.getServiceKeys(query)` 和 `getInstances(query)` 完整快照。
- Produces: `pageServices(query, pageQuery)`、`pageInstances(query, pageQuery)`、`/registry/services/page`、`/registry/instances/page`。

- [ ] **Step 1: 写 Adapter 失败测试**

```java
@Test
void pagesSortedServiceKeysWithoutChangingCatalog() {
    DdcManagementServiceKey a = service("infra", "gateway-a", "svc-a");
    DdcManagementServiceKey b = service("infra", "gateway-b", "svc-b");
    when(facade.getServiceKeys(query)).thenReturn(
            new DdcManagementServiceCatalog(9, Instant.EPOCH, List.of(b, a))
    );

    Page<DdcManagementServiceKey> page = service.pageServices(
            query, new PageQuery(1, 1)
    );

    assertThat(page.getContent()).containsExactly(a);
    assertThat(page.getTotalElements()).isEqualTo(2);
    verify(facade).getServiceKeys(query);
}
```

实例测试传入乱序实例并断言 `status/host/port/instanceId` 稳定顺序和 total。

- [ ] **Step 2: 运行 Adapter 测试并确认失败**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcRegistryAdminPageServiceTest,DdcRegistryAdminControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，Page Service 和 Page routes 不存在。

- [ ] **Step 3: 实现 Registry Admin Page Service**

```java
@Service
public class DdcRegistryAdminPageService {

    private static final Comparator<String> TEXT =
            Comparator.nullsFirst(String::compareTo);

    private static final Comparator<DdcManagementServiceKey> SERVICE_ORDER =
            Comparator.comparing(DdcManagementServiceKey::bizCode, TEXT)
                    .thenComparing(DdcManagementServiceKey::env, TEXT)
                    .thenComparing(DdcManagementServiceKey::appCode, TEXT)
                    .thenComparing(DdcManagementServiceKey::serviceKind, TEXT)
                    .thenComparing(DdcManagementServiceKey::protocol, TEXT)
                    .thenComparing(DdcManagementServiceKey::serviceName, TEXT)
                    .thenComparing(DdcManagementServiceKey::group, TEXT)
                    .thenComparing(DdcManagementServiceKey::version, TEXT)
                    .thenComparing(DdcManagementServiceKey::serviceId, TEXT);

    private final DdcManagementFacade facade;

    public Page<DdcManagementServiceKey> pageServices(
            DdcManagementServiceQuery query,
            PageQuery pageQuery) {
        List<DdcManagementServiceKey> records = facade.getServiceKeys(query)
                .services().stream().sorted(SERVICE_ORDER).toList();
        return DdcAdminPageSupport.slice(records, pageQuery);
    }
}
```

在同一类中增加实例比较器和分页方法；该类不得直接依赖 Redis Repository：

```java
private static final Comparator<DdcManagementServiceInstance> INSTANCE_ORDER =
        Comparator.comparing(DdcManagementServiceInstance::status, TEXT)
                .thenComparing(DdcManagementServiceInstance::host, TEXT)
                .thenComparingInt(DdcManagementServiceInstance::port)
                .thenComparing(DdcManagementServiceInstance::instanceId, TEXT);

public Page<DdcManagementServiceInstance> pageInstances(
        DdcManagementServiceQuery query,
        PageQuery pageQuery) {
    List<DdcManagementServiceInstance> records = facade.getInstances(query)
            .instances().stream().sorted(INSTANCE_ORDER).toList();
    return DdcAdminPageSupport.slice(records, pageQuery);
}
```

- [ ] **Step 4: 增加两个 Controller Page 路由**

复用当前私有 `query(...)` 构造方法：

```java
@GetMapping("/services/page")
public PageResultRecord<DdcManagementServiceKey> pageServices(
        @RequestParam(value = "bizCode", required = false) String bizCode,
        @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
        @RequestParam(value = "env", required = false) String env,
        @RequestParam(value = "appCode", required = false) String appCode,
        @RequestParam(value = "serviceKind", required = false) String serviceKind,
        @RequestParam(value = "protocol", required = false) String protocol,
        @RequestParam(value = "serviceName", required = false) String serviceName,
        @RequestParam(value = "group", required = false) String group,
        @RequestParam(value = "version", required = false) String version,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(pageService.pageServices(
            query(bizCode, namespaceCode, env, appCode, serviceKind,
                    protocol, serviceName, group, version),
            pageQuery
    ));
}
```

`/instances/page` 使用原 instances 的必填 service key 参数：

```java
@GetMapping("/instances/page")
public PageResultRecord<DdcManagementServiceInstance> pageInstances(
        @RequestParam("bizCode") String bizCode,
        @RequestParam("env") String env,
        @RequestParam("appCode") String appCode,
        @RequestParam("serviceKind") String serviceKind,
        @RequestParam("protocol") String protocol,
        @RequestParam("serviceName") String serviceName,
        @RequestParam(value = "group", required = false) String group,
        @RequestParam(value = "version", required = false) String version,
        PageQuery pageQuery) {
    return DdcAdminPageSupport.result(pageService.pageInstances(
            query(bizCode, null, env, appCode, serviceKind,
                    protocol, serviceName, group, version),
            pageQuery
    ));
}
```

不得修改原 `services(...)` 和 `instances(...)`。

- [ ] **Step 5: 锁定旧接口和 RPC 边界**

Controller 测试同时断言：

```java
mockMvc.perform(get("/api/v1/ddc/registry/services/page")
        .param("pageNo", "1").param("pageSize", "10"))
        .andExpect(jsonPath("$.records").isArray())
        .andExpect(jsonPath("$.page.total").value(1));

mockMvc.perform(get("/api/v1/ddc/registry/services"))
        .andExpect(jsonPath("$.data.generation").exists())
        .andExpect(jsonPath("$.data.services").isArray());
```

运行源码扫描：

```bash
! rg -n "Page(Query|ResultRecord)" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main
```

- [ ] **Step 6: 运行 Task 6 测试**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcRegistryAdminPageServiceTest,DdcRegistryAdminControllerTest,DdcManagementRpcProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS，0 failures；源码扫描无匹配。

- [ ] **Step 7: 提交 Task 6**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/management/DdcRegistryAdminPageService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/register/DdcRegistryAdminController.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/management/DdcRegistryAdminPageServiceTest.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryAdminControllerTest.java
git commit -m "feat(ddc): add paged registry admin views"
```

---

### Task 7: Frontend Page API、QueryClient 与 Scope Option Cache

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/types.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.test.ts`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/query/queryClient.ts`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/hooks/usePageState.ts`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/hooks/usePageState.test.ts`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/test/renderWithQueryClient.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/useScopeOptions.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/BizSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/NamespaceSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/EnvSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/AppSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/useScopeOptions.test.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/ScopeSelects.test.tsx`

**Interfaces:**
- Consumes: Backend `PageResultRecord<T>` from Tasks 1–6；现有 OAuth refresh 和 `DdcApiError`。
- Produces: `ddcPageApi<T>`、AbortSignal request option、QueryClient、`usePageState`、React Query scope option keys。

- [ ] **Step 1: 写 `ddcPageApi` 失败测试**

```ts
const pageRecord = <T,>(records: T[], total = records.length) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
  page: {
    total,
    pageNo: 2,
    pageSize: 20,
    pages: 2,
    hasNext: false,
    hasPrevious: true,
  },
  traceId: 'trace-page',
  timestamp: 1,
})

it('returns page records and forwards AbortSignal', async () => {
  const controller = new AbortController()
  vi.mocked(fetch).mockResolvedValue(jsonResponse(pageRecord([{ id: 'b1' }], 21)))

  await expect(ddcPageApi<{ id: string }>('/api/v1/ddc/bizs/page', {
    signal: controller.signal,
  })).resolves.toMatchObject({
    records: [{ id: 'b1' }],
    page: { total: 21, pageNo: 2, pageSize: 20 },
  })

  expect(vi.mocked(fetch).mock.calls[0][1]?.signal).toBe(controller.signal)
})

it('accepts ResultRecord failures from the global exception handler', async () => {
  vi.mocked(fetch).mockResolvedValue(jsonResponse({
    success: false,
    code: 422,
    status: 'INVALID_REQUEST',
    message: '请求参数无效',
    data: null,
    traceId: 'trace-error',
    timestamp: 1,
  }))

  await expect(ddcPageApi('/api/v1/ddc/bizs/page'))
    .rejects.toMatchObject({ code: '422', traceId: 'trace-error' })
})
```

保留并更新现有 HTTP failure、401 refresh、网络失败和 traceId tests，使它们经过 `requestEnvelope`；401 test 同时断言 refresh 后的第二次 fetch 继续携带原 `AbortSignal`。

- [ ] **Step 2: 运行 Client 测试并确认失败**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/api/client.test.ts
```

Expected: FAIL，`ddcPageApi` 和 `signal` option 不存在。

- [ ] **Step 3: 定义前端 Page 类型并重构共享请求层**

```ts
export type ResultEnvelope = {
  success: boolean
  code: number
  status: string
  message: string
  traceId?: string
  timestamp: number
}

export type ResultRecord<T> = ResultEnvelope & { data: T }

export type PageMetaRecord = {
  total: number
  pageNo: number
  pageSize: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

export type PageResultRecord<T> = ResultEnvelope & {
  records: T[]
  page: PageMetaRecord
}
```

```ts
export type DdcRequestOptions = {
  method?: string
  body?: unknown
  signal?: AbortSignal
}

const requestEnvelope = async (
  path: string,
  options: DdcRequestOptions,
): Promise<ResultEnvelope & Record<string, unknown>> => {
  const headers = new Headers()
  headers.set('Authorization', `Bearer ${tokenProvider()}`)
  let body: string | undefined
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }

  const request = () => fetch(path, {
    method: options.method ?? 'GET',
    headers,
    body,
    signal: options.signal,
  })

  let response: Response
  try {
    response = await request()
    if (response.status === 401) {
      try {
        headers.set('Authorization', `Bearer ${await oauthClient.refresh()}`)
        response = await request()
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          throw error
        }
      }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new DdcApiError(
      0,
      'DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE',
      '无法连接 DDC 管理端',
    )
  }

  const payload = (await response.json().catch(() => ({}))) as
    Partial<ResultEnvelope> & Record<string, unknown>
  if (response.status === 401) {
    unauthorizedHandler()
    throw new DdcApiError(
      401,
      'UNAUTHORIZED',
      '统一身份登录已过期，请重新登录',
      payload.traceId,
    )
  }
  if (!response.ok || payload.success === false) {
    const errorStatus = response.ok ? 500 : response.status
    throw new DdcApiError(
      errorStatus,
      String(payload.code ?? errorStatus),
      payload.message || String(payload.code) || `请求失败 (${response.status})`,
      payload.traceId,
    )
  }
  return payload as ResultEnvelope & Record<string, unknown>
}

export async function ddcApi<T>(
  path: string,
  options: DdcRequestOptions = {},
): Promise<T> {
  const payload = await requestEnvelope(path, options)
  return payload.data as T
}

export async function ddcPageApi<T>(
  path: string,
  options: DdcRequestOptions = {},
): Promise<PageResultRecord<T>> {
  const payload = await requestEnvelope(path, options)
  if (!Array.isArray(payload.records)
      || payload.page === null
      || typeof payload.page !== 'object') {
    throw new DdcApiError(500, 'DDC_INVALID_PAGE_RESPONSE', '分页响应格式无效', payload.traceId as string | undefined)
  }
  return payload as PageResultRecord<T>
}
```

- [ ] **Step 4: 写 `usePageState` 失败测试**

```ts
it('resets page number when filters or page size change', () => {
  const { result } = renderHook(() => usePageState())

  act(() => result.current.onTableChange(3, 10))
  expect(result.current.page).toEqual({ pageNo: 3, pageSize: 10 })

  act(() => result.current.resetPage())
  expect(result.current.page).toEqual({ pageNo: 1, pageSize: 10 })

  act(() => result.current.onTableChange(2, 20))
  expect(result.current.page).toEqual({ pageNo: 1, pageSize: 20 })
})
```

- [ ] **Step 5: 实现 QueryClient、测试渲染器和 Page state**

```ts
export const createDdcQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false, staleTime: 30_000 },
    mutations: { retry: false },
  },
})

export const queryClient = createDdcQueryClient()
```

```ts
export function usePageState(initialPageSize = 10) {
  const [page, setPage] = useState({ pageNo: 1, pageSize: initialPageSize })
  const resetPage = useCallback(() => {
    setPage((current) => ({ ...current, pageNo: 1 }))
  }, [])
  const onTableChange = useCallback((pageNo: number, pageSize: number) => {
    setPage((current) => ({
      pageNo: current.pageSize === pageSize ? pageNo : 1,
      pageSize,
    }))
  }, [])
  return { page, resetPage, onTableChange }
}
```

测试渲染器和生产 Provider 使用以下固定结构：

```tsx
export function renderWithQueryClient(ui: ReactElement) {
  const client = createDdcQueryClient()
  return {
    queryClient: client,
    ...render(
      <QueryClientProvider client={client}>
        <AntdApp>{ui}</AntdApp>
      </QueryClientProvider>,
    ),
  }
}
```

`main.tsx` 在 `AdminThemeProvider` 内、`AntdApp` 外增加生产 `QueryClientProvider`：

```tsx
<StrictMode>
  <I18nProvider>
    <AdminThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AntdApp>
          <App />
        </AntdApp>
      </QueryClientProvider>
    </AdminThemeProvider>
  </I18nProvider>
</StrictMode>
```

- [ ] **Step 6: 把 Scope options 迁移到 React Query cache**

定义稳定 key：

```ts
export const scopeOptionQueryKey = ['ddc', 'scope-options'] as const

export const scopeOptionKey = (path: string) => [
  ...scopeOptionQueryKey,
  path,
] as const
```

`useScopeOption(path)` 使用：

```ts
return useQuery({
  queryKey: scopeOptionKey(path),
  queryFn: ({ signal }) => ddcApi<unknown>(path, { signal }).then(toOptions),
})
```

四个 Select 各自只调用所需路径，不再通过一个 hook 同时加载四类选项。保留 `withParams` 和实体到 `{ value, label }` 的映射语义。删除 Promise Map 和 `clearScopeOptionsCache`，更新 tests 使用 `renderWithQueryClient`。

- [ ] **Step 7: 运行 Task 7 tests、typecheck 和 lint**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/api/client.test.ts src/hooks/usePageState.test.ts src/components/scope/useScopeOptions.test.ts src/components/scope/ScopeSelects.test.tsx
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run typecheck
npm run lint
```

Expected: tests PASS，typecheck exit 0，lint exit 0。

- [ ] **Step 8: 提交 Task 7**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/types.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.test.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/query/queryClient.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/hooks/usePageState.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/hooks/usePageState.test.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/test/renderWithQueryClient.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope
git commit -m "feat(ddc-web): add paged query infrastructure"
```

---

### Task 8: Metadata Pages 与 Namespace Binding Drawer

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/page/AdminPageHeader.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/BizsPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/EnvPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/AppsPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/NamespacesPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/BizsPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/EnvPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/AppsPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/NamespacesPage.test.tsx`

**Interfaces:**
- Consumes: `ddcPageApi`、QueryClient、`usePageState`、scope option query keys。
- Produces: 四个 Metadata Page 使用服务端分页；Namespace Binding 使用响应式 Drawer + multiple Select。

- [ ] **Step 1: 先把 Metadata tests 改成 Page contract 并确认失败**

统一 test payload：

```ts
const pageRecord = <T,>(records: T[], total = records.length) => ({
  success: true,
  code: 0,
  status: 'SUCCESS',
  message: '',
  records,
  page: {
    total,
    pageNo: 1,
    pageSize: 10,
    pages: Math.ceil(total / 10),
    hasNext: total > 10,
    hasPrevious: false,
  },
  traceId: 't',
  timestamp: 1,
})
```

Biz test 断言真实 total 和翻页请求：

```ts
renderWithQueryClient(<BizsPage />)
expect(await screen.findByText('支付业务域')).toBeInTheDocument()
expect(screen.getByText('共 21 条')).toBeInTheDocument()

fireEvent.click(screen.getByTitle('2'))
await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/bizs/page?pageNo=2&pageSize=10'),
  expect.anything(),
))
```

Env/App/Namespace 分别断言 `/page`、筛选提交后 `pageNo=1`、Reset 清空条件。

Biz test 再保存第一次查询的 `AbortSignal`，连续提交两个 keyword 后断言第一次 signal 的 `aborted` 为 true，最后只渲染第二次响应。另让当前第 2 页只返回一条记录，确认删除后下一次请求包含 `pageNo=1`。每个页面至少覆盖一次 error Alert 的“重试”按钮并断言点击后重新发起同一 Page 请求。

- [ ] **Step 2: 增加 Namespace Binding Drawer 失败测试**

先将 `window.matchMedia` 设置为不命中 `md` breakpoint，再执行：

```ts
fireEvent.click(await screen.findByRole('button', { name: '管理绑定' }))

expect(await screen.findByRole('dialog', { name: /管理绑定/ })).toBeInTheDocument()
expect(document.querySelector('.ant-checkbox-group')).not.toBeInTheDocument()
expect(document.querySelector('.ant-select-multiple')).toBeInTheDocument()
expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/namespace-env-app-bindings?'),
  expect.anything(),
)
expect(document.querySelector('.ant-drawer-content-wrapper'))
  .toHaveStyle({ width: '100%' })
```

- [ ] **Step 3: 运行四个页面测试并确认失败**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/BizsPage.test.tsx src/pages/EnvPage.test.tsx src/pages/AppsPage.test.tsx src/pages/NamespacesPage.test.tsx
```

Expected: FAIL，页面仍读取 `data` List 且使用客户端 pagination。

- [ ] **Step 4: 实现统一 Metadata Query 结构**

每个页面维护 `draftFilters`、`submittedFilters` 和 `usePageState`：

```ts
const pageState = usePageState()
const queryString = buildQuery({
  ...submittedFilters,
  pageNo: pageState.page.pageNo,
  pageSize: pageState.page.pageSize,
})
const query = useQuery({
  queryKey: ['ddc', 'bizs', submittedFilters, pageState.page],
  queryFn: ({ signal }) => ddcPageApi<DdcBiz>(
    `/api/v1/ddc/bizs/page?${queryString}`,
    { signal },
  ),
  placeholderData: keepPreviousData,
})
```

查询提交执行：

```ts
setSubmittedFilters({ ...draftFilters })
pageState.resetPage()
```

Table pagination：

```tsx
pagination={{
  current: query.data?.page.pageNo ?? pageState.page.pageNo,
  pageSize: query.data?.page.pageSize ?? pageState.page.pageSize,
  total: query.data?.page.total ?? 0,
  showSizeChanger: true,
  pageSizeOptions: [10, 20, 50],
  showTotal: (total) => `共 ${total} 条`,
  onChange: pageState.onTableChange,
}}
scroll={{ x: 'max-content' }}
```

- [ ] **Step 5: 迁移 Metadata mutations 和反馈**

四个页面的列表 query key 前缀固定为 `bizs`、`envs`、`apps`、`namespaces`。Biz 页面 mutation 的成功回调写为：

```ts
await queryClient.invalidateQueries({ queryKey: ['ddc', 'bizs'] })
await queryClient.invalidateQueries({ queryKey: scopeOptionQueryKey })
message.success('业务域保存成功')
```

Env、App、Namespace 页面分别将列表 key 和成功文案替换为 `envs/环境保存成功`、`apps/应用保存成功`、`namespaces/命名空间保存成功`；删除、启停操作使用对应资源名的完成文案，不使用运行期 `resourceName` 或 `successText` 占位变量。

Switch mutation 期间禁用当前行；删除使用 `Popconfirm`。删除完成后，当当前页只有一条且 `pageNo > 1` 时调用 `onTableChange(pageNo - 1, pageSize)`。

- [ ] **Step 6: 实现 `AdminPageHeader` 和页面状态**

```tsx
type Props = {
  title: string
  description: string
  extra?: ReactNode
}

export default function AdminPageHeader({ title, description, extra }: Props) {
  return (
    <Flex justify="space-between" align="flex-start" gap={16} wrap>
      <div>
        <Typography.Title level={3}>{title}</Typography.Title>
        <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>
      </div>
      {extra}
    </Flex>
  )
}
```

每个表格 Card 内使用 `PageState` 展示 pending/error/empty/retry；保留旧数据刷新时 Table 使用 `loading={query.isFetching}`。

- [ ] **Step 7: 实现 Namespace Binding Drawer**

使用 `Drawer`：

```tsx
<Drawer
  open={bindingNamespace !== null}
  title={`管理绑定：${bindingNamespace?.bizCode ?? ''}/${bindingNamespace?.namespaceCode ?? ''}`}
  width={screens.md ? 860 : '100%'}
  onClose={closeBindings}
  extra={<Button type="primary" loading={bindingSaving} onClick={() => void saveBindings()}>保存绑定</Button>}
>
```

应用列使用：

```tsx
<Select
  mode="multiple"
  showSearch
  maxTagCount="responsive"
  optionFilterProp="label"
  value={bindingDraft[env.envCode] ?? []}
  options={bindingApps.map((app) => ({
    value: app.appCode,
    label: `${app.appCode}（${app.appName}）`,
  }))}
  onChange={(values) => setBindingDraft((current) => ({
    ...current,
    [env.envCode]: values,
  }))}
  style={{ width: '100%' }}
/>
```

绑定数据、完整 Env/App 选项继续调用原 List 接口，不调用 `/page`。

- [ ] **Step 8: 运行 Task 8 tests、typecheck 和 lint**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/BizsPage.test.tsx src/pages/EnvPage.test.tsx src/pages/AppsPage.test.tsx src/pages/NamespacesPage.test.tsx
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run typecheck
npm run lint
```

Expected: all PASS/exit 0。

- [ ] **Step 9: 提交 Task 8**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/page/AdminPageHeader.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/BizsPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/EnvPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/AppsPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/NamespacesPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/BizsPage.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/EnvPage.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/AppsPage.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/NamespacesPage.test.tsx
git commit -m "feat(ddc-web): modernize metadata pages"
```

---

### Task 9: Config Page 与 Version Drawer 分页

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigsPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigsPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigEditorDialog.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigEditorDialog.test.tsx`

**Interfaces:**
- Consumes: `/configs/page`、`/configs/{id}/versions/page`、Task 7 query infrastructure。
- Produces: Config 主表和 Version Drawer 独立服务端分页；Ant Design 确认交互。

- [ ] **Step 1: 写 Config Page 失败测试**

```ts
renderWithQueryClient(<ConfigsPage />)

await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/configs/page?'),
  expect.anything(),
))
expect(await screen.findByText('共 13 条')).toBeInTheDocument()

fireEvent.click(screen.getByRole('button', { name: '更多操作' }))
fireEvent.click(await screen.findByRole('menuitem', { name: '查看版本' }))
await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/configs/cfg-1/versions/page?pageNo=1&pageSize=10'),
  expect.anything(),
))
```

发布和删除测试断言存在 Ant Design confirmation 文案，且确认前没有发 POST/DELETE。`ConfigEditorDialog.test.tsx` 在窄屏 breakpoint 下打开编辑器，断言 Modal width 是 `calc(100vw - 24px)`。

- [ ] **Step 2: 运行测试并确认失败**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/ConfigsPage.test.tsx src/pages/ConfigEditorDialog.test.tsx
```

Expected: FAIL，页面仍请求 List 和 `window.confirm`。

- [ ] **Step 3: 迁移 Config 主 Query**

使用 `submittedScope + usePageState + useQuery` 请求：

```ts
ddcPageApi<DdcConfig>(
  `/api/v1/ddc/configs/page?${buildQuery({
    ...submittedScope,
    includeDeleted: false,
    pageNo: page.pageNo,
    pageSize: page.pageSize,
  })}`,
  { signal },
)
```

删除客户端 `Map` 去重；数据库 Page 已以 id 唯一。Table 显示 `page.total`，YAML 内容列配置 `ellipsis` 和最大宽度。操作列固定右侧：编辑和发布作为直接按钮，Ant Design `Dropdown` 的菜单项固定为“查看版本”和“删除”，删除项打开 `Popconfirm`，整个操作区用 `Space.Compact` 且禁止换行。

- [ ] **Step 4: 迁移 Version Drawer Query**

为当前 config 保存独立 `versionPage`：

```ts
const versionsQuery = useQuery({
  enabled: versionsConfig !== null,
  queryKey: ['ddc', 'config-versions', versionsConfig?.id, versionPage],
  queryFn: ({ signal }) => ddcPageApi<DdcConfigVersion>(
    `/api/v1/ddc/configs/${encodeURIComponent(versionsConfig!.id)}/versions/page?${buildQuery(versionPage)}`,
    { signal },
  ),
  placeholderData: keepPreviousData,
})
```

使用响应式 Drawer：desktop 860px，窄屏 100%；关闭时重置 version page。Config Editor 使用 `Grid.useBreakpoint()`，Modal 宽度固定为 `screens.md ? 860 : 'calc(100vw - 24px)'`，表单内容允许纵向滚动但不得产生页面级横向滚动。Config 和 Version 两张 Table 都设置 `scroll={{ x: 'max-content' }}`。

- [ ] **Step 5: 替换确认和 mutation 流程**

发布使用 `Modal.confirm`：

```ts
modal.confirm({
  title: `确认发布 ${config.resourceName} 当前版本？`,
  okText: '发布',
  onOk: () => publishMutation.mutateAsync(config),
})
```

删除和回滚使用 `Popconfirm` 或 `modal.confirm`。所有消息从 `const { message, modal } = App.useApp()` 获取。Config Editor 保存后失效 `['ddc', 'configs']` 和 `scopeOptionQueryKey`。

- [ ] **Step 6: 运行 Task 9 tests、typecheck 和 lint**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/ConfigsPage.test.tsx src/pages/ConfigEditorDialog.test.tsx
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run typecheck
npm run lint
```

Expected: all PASS/exit 0。

- [ ] **Step 7: 提交 Task 9**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigsPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigsPage.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigEditorDialog.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigEditorDialog.test.tsx
git commit -m "feat(ddc-web): paginate config management"
```

---

### Task 10: Publish Task 与 Cache 页面现代化

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/PublishTasksPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/PublishTasksPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/CachePage.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/CachePage.test.tsx`

**Interfaces:**
- Consumes: `/publish-tasks/page`、`/cache/check/page`、Task 7 query infrastructure。
- Produces: 当前页轮询的发布任务、带状态筛选的服务端分页、分页缓存检查和 Ant Design 重建确认。

- [ ] **Step 1: 写 Publish 当前页轮询失败测试**

使用 fake timers：

```ts
vi.useFakeTimers()
renderWithQueryClient(<PublishTasksPage />)

await vi.runOnlyPendingTimersAsync()
await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/publish-tasks/page?pageNo=1&pageSize=10'),
  expect.anything(),
))

fireEvent.change(screen.getByLabelText('状态'), { target: { value: 'FAILED' } })
fireEvent.click(screen.getByRole('button', { name: '查询' }))
await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('status=FAILED'),
  expect.anything(),
))
```

对 background refetch 返回失败，断言旧任务仍显示且没有重复 message DOM。
在窄屏 breakpoint 下打开任务详情，断言 Modal width 是 `calc(100vw - 24px)`。

- [ ] **Step 2: 写 Cache Page 失败测试**

```ts
renderWithQueryClient(<CachePage />)
const chooseScopeValue = (input: HTMLElement, value: string) => {
  fireEvent.change(input, { target: { value } })
  fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' })
}
const [bizInput, namespaceInput, envInput, appInput] =
  screen.getAllByRole('combobox')
chooseScopeValue(bizInput, 'infra')
chooseScopeValue(namespaceInput, 'default')
chooseScopeValue(envInput, 'prod')
chooseScopeValue(appInput, 'gateway')
fireEvent.click(screen.getByRole('button', { name: '检查缓存' }))

await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/cache/check/page?'),
  expect.anything(),
))
expect(await screen.findByText('共 12 条')).toBeInTheDocument()

fireEvent.click(screen.getByRole('button', { name: '重建缓存' }))
expect(screen.getByText('确认重建该作用域下的缓存？')).toBeInTheDocument()
```

- [ ] **Step 3: 运行 tests 并确认失败**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/PublishTasksPage.test.tsx src/pages/CachePage.test.tsx
```

Expected: FAIL，Publish 仍全量请求，Cache test/file/page route 不存在。

- [ ] **Step 4: 实现 Publish 筛选与轮询 Query**

筛选结构：

```ts
type PublishTaskFilter = ScopeValue & {
  status: string
  changeId: string
}
```

Query：

```ts
useQuery({
  queryKey: ['ddc', 'publish-tasks', submittedFilters, page],
  queryFn: ({ signal }) => ddcPageApi<DdcPublishTask>(
    `/api/v1/ddc/publish-tasks/page?${buildQuery({
      ...submittedFilters,
      pageNo: page.pageNo,
      pageSize: page.pageSize,
    })}`,
    { signal },
  ),
  placeholderData: keepPreviousData,
  refetchInterval: 15_000,
  refetchIntervalInBackground: true,
})
```

background error 通过 `PageState` 的 `showPartial` 分支加 Card 内 `Alert` 表示；不得在 interval error 上调用 `message.error`。Retry 使用 `Modal.confirm`，成功后 invalidate 当前任务 Page。Publish Table 设置 `scroll={{ x: 'max-content' }}`；详情 Modal 使用 `screens.md ? 720 : 'calc(100vw - 24px)'`。

- [ ] **Step 5: 实现 Cache Page Query 和摘要**

只在用户提交完整物理 scope 后启用 query。请求含 `pageNo/pageSize`，Table 受控。当前页摘要：

```ts
const matched = page.records.filter((row) => row.matched).length
const mismatched = page.records.length - matched
```

使用两个 `Statistic` 显示“本页一致”和“本页不一致”，标题明确本页范围。Cache Table 设置 `scroll={{ x: 'max-content' }}`。重建使用 `Modal.confirm`；成功后 invalidate `['ddc', 'cache-check']`。

- [ ] **Step 6: 运行 Task 10 tests、typecheck 和 lint**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/PublishTasksPage.test.tsx src/pages/CachePage.test.tsx
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run typecheck
npm run lint
```

Expected: all PASS/exit 0。

- [ ] **Step 7: 提交 Task 10**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/PublishTasksPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/PublishTasksPage.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/CachePage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/CachePage.test.tsx
git commit -m "feat(ddc-web): modernize publish and cache pages"
```

---

### Task 11: Registry 懒加载与响应式 Admin Shell

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/RegistryPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/RegistryPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/AuthContext.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/AuthContext.test.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/styles/admin.css`

**Interfaces:**
- Consumes: `/registry/services/page`、`/registry/instances/page`、`AdminPageHeader`。
- Produces: 服务行分页、单服务实例 Drawer 懒加载、桌面折叠 Sider、窄屏 Drawer 导航、全局溢出防护。

- [ ] **Step 1: 写 Registry 单服务懒加载失败测试**

先将 `window.matchMedia` 设置为不命中 `md` breakpoint，再执行：

```ts
renderWithQueryClient(<RegistryPage />)

expect(await screen.findByText('orders.OrderService')).toBeInTheDocument()
expect(fetch).not.toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/registry/instances/page'),
  expect.anything(),
)

fireEvent.click(screen.getByRole('button', { name: '查看实例' }))
await waitFor(() => expect(fetch).toHaveBeenCalledWith(
  expect.stringContaining('/api/v1/ddc/registry/instances/page?'),
  expect.anything(),
))

const instanceCalls = vi.mocked(fetch).mock.calls.filter(([url]) =>
  String(url).includes('/registry/instances/page'))
expect(instanceCalls).toHaveLength(1)
expect(document.querySelector('.ant-drawer-content-wrapper'))
  .toHaveStyle({ width: '100%' })
```

- [ ] **Step 2: 写响应式 Shell 失败测试**

在 `AdminLayout.test.tsx` mock `useAuth()` 返回 `identity: 'Mario'` 和 `logout`，并用 `MemoryRouter + Routes` 直接渲染 Layout。用以下 helper 精确模拟 Ant Design 的 min-width media queries：

```ts
const setViewport = (width: number) => {
  window.matchMedia = vi.fn().mockImplementation((query: string) => {
    const minWidth = Number(query.match(/min-width:\s*(\d+)px/)?.[1] ?? 0)
    return {
      matches: minWidth > 0 && width >= minWidth,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }
  })
}
```

窄屏测试执行 `setViewport(600)` 后断言：

```ts
expect(screen.queryByLabelText('桌面主导航')).not.toBeInTheDocument()
fireEvent.click(screen.getByRole('button', { name: '打开导航' }))
expect(await screen.findByLabelText('移动主导航')).toBeInTheDocument()
expect(screen.getByText('元数据管理')).toBeInTheDocument()
```

Desktop 测试执行 `setViewport(1280)`，断言 `桌面主导航`、`折叠导航`、`Mario`、`DDC 已连接` 和带 icon 的菜单存在。

`AuthContext.test.ts` 构造包含 `displayName: 'Mario'` 的三段 JWT，断言 `identityFromToken(token) === 'Mario'`；另断言格式错误 token 返回空字符串。

- [ ] **Step 3: 运行 tests 并确认失败**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/RegistryPage.test.tsx src/layouts/AdminLayout.test.tsx src/auth/AuthContext.test.ts
```

Expected: FAIL，Registry 仍应用聚合并发加载实例，Layout 无移动导航。

- [ ] **Step 4: 迁移 Registry 服务目录 Page**

删除 `AppRow` 客户端聚合，Table 直接显示 `RegistryService`：

```ts
const servicesQuery = useQuery({
  queryKey: ['ddc', 'registry-services', submittedScope, servicePage],
  queryFn: ({ signal }) => ddcPageApi<RegistryService>(
    `/api/v1/ddc/registry/services/page?${buildQuery({
      ...submittedScope,
      pageNo: servicePage.pageNo,
      pageSize: servicePage.pageSize,
    })}`,
    { signal },
  ),
  placeholderData: keepPreviousData,
})
```

列为 `biz/env/app`、`serviceKind/protocol`、`serviceName`、`group/version`、`serviceId` 和“查看实例”。服务总数使用 `page.total`，服务和实例 Table 都设置 `scroll={{ x: 'max-content' }}`。

- [ ] **Step 5: 实现单服务实例 Drawer Page**

保存 `selectedService` 和独立 `instancePage`，仅在 Drawer 打开时启用：

```ts
const instancesQuery = useQuery({
  enabled: selectedService !== null,
  queryKey: ['ddc', 'registry-instances', selectedService?.serviceId, instancePage],
  queryFn: ({ signal }) => ddcPageApi<RegistryInstance>(
    `/api/v1/ddc/registry/instances/page?${buildQuery({
      bizCode: selectedService!.bizCode,
      env: selectedService!.env,
      appCode: selectedService!.appCode,
      serviceKind: selectedService!.serviceKind,
      protocol: selectedService!.protocol,
      serviceName: selectedService!.serviceName,
      group: selectedService!.group,
      version: selectedService!.version,
      pageNo: instancePage.pageNo,
      pageSize: instancePage.pageSize,
    })}`,
    { signal },
  ),
})
```

Drawer 窄屏宽度 100%，Desktop 860px；关闭时清空 service 并重置实例页。

- [ ] **Step 6: 实现响应式 Layout 和 Menu 分组**

Menu 定义使用 Ant Design icons：

```ts
const menuItems: MenuProps['items'] = [
  {
    type: 'group',
    label: '运行状态',
    children: [
      { key: 'registry', icon: <ClusterOutlined />, label: '服务注册' },
      { key: 'publish-tasks', icon: <DeploymentUnitOutlined />, label: '发布任务' },
      { key: 'cache', icon: <DatabaseOutlined />, label: '缓存' },
    ],
  },
  {
    type: 'group',
    label: '配置管理',
    children: [
      { key: 'configs', icon: <FileTextOutlined />, label: '配置资源' },
    ],
  },
  {
    type: 'group',
    label: '元数据管理',
    children: [
      { key: 'bizs', icon: <PartitionOutlined />, label: '业务域' },
      { key: 'envs', icon: <CloudOutlined />, label: '环境' },
      { key: 'apps', icon: <AppstoreOutlined />, label: '应用' },
      { key: 'namespaces', icon: <ApartmentOutlined />, label: '命名空间' },
    ],
  },
]
```

使用 `Grid.useBreakpoint()`：`md` 以上渲染带 `aria-label="桌面主导航"` 的可折叠 Sider，`md` 以下 Header 显示 `aria-label="打开导航"` 的 Menu 按钮并打开包含 `aria-label="移动主导航"` Menu 的 Drawer。Header 展示当前菜单 label、连接 Badge、`useAuth().identity` 和退出。

`AuthContextValue` 增加只读 `identity: string`。使用 Admin Web Shared 已导出的 `decodeTokenPayload(token)`，依次取非空字符串 `displayName`、`name`、`preferred_username`、`sub`；无 token 或解码失败返回空字符串。该派生值放在 `useMemo`，不新增身份接口调用：

```ts
export const identityFromToken = (token: string): string => {
  if (!token) return ''
  try {
    const claims = decodeTokenPayload(token)
    return [claims.displayName, claims.name, claims.preferred_username, claims.sub]
      .find((value): value is string => typeof value === 'string' && value.trim() !== '')
      ?? ''
  } catch {
    return ''
  }
}

const identity = useMemo(() => identityFromToken(token), [token])
```

- [ ] **Step 7: 增加最小响应式 CSS**

```css
.ddc-admin-layout,
.ddc-admin-main,
.ddc-admin-content {
  min-width: 0;
}

.ddc-admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
}

.ddc-admin-content {
  padding: 24px;
  overflow-x: hidden;
}

.ddc-admin-table-card .ant-card-body {
  min-width: 0;
  overflow-x: auto;
}

@media (max-width: 767px) {
  .ddc-admin-content {
    padding: 12px;
  }
}
```

在 `AdminLayout` 通过 `theme.useToken()` 取得 `token.colorBgContainer` 和 `token.colorBorderSecondary`，把 Header 背景及下边框设置在组件 style；在 `main.tsx` 引入 `./styles/admin.css` 一次。不得增加硬编码第二套颜色 token。

- [ ] **Step 8: 运行 Task 11 tests、typecheck、lint、build**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test -- src/pages/RegistryPage.test.tsx src/layouts/AdminLayout.test.tsx src/auth/AuthContext.test.ts
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run typecheck
npm run lint
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run build
```

Expected: tests PASS，typecheck/lint/build exit 0。允许报告现有 Vite chunk-size warning，但不能把 warning 隐藏为成功条件。

- [ ] **Step 9: 提交 Task 11**

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/RegistryPage.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/RegistryPage.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/AuthContext.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/AuthContext.test.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/styles/admin.css
git commit -m "feat(ddc-web): modernize registry and admin shell"
```

---

### Task 12: Active Documentation、全量验证与残留清理

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/README.md`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/README.zh-CN.md`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.zh-CN.md`

**Interfaces:**
- Consumes: Tasks 1–11 的 12 个 Page API 和完成后的 Admin Web。
- Produces: 当前 README 分页契约说明、完整验证证据、零生产残留。

- [ ] **Step 1: 更新英文和中文 Admin Web README**

英文新增“Server-side pagination”章节，内容固定为：

```markdown
## Server-side pagination

Management tables call the additive `/page` endpoints and read
`PageResultRecord.records` plus `PageResultRecord.page`. Page numbers start at
1; the UI offers 10, 20, or 50 rows per page. Scope selectors and namespace
binding editors intentionally keep using the legacy list endpoints because
they require complete option sets.

Registry tables page service keys and lazily page instances for one selected
service. DDC Starter and RPC clients continue to consume complete catalogs and
snapshots; Admin Web pagination is not part of the machine RPC contract.
```

中文增加语义完全对应的“服务端分页”章节，明确 `/page`、`records/page`、页码从 1 开始、下拉仍使用完整 List、RPC 完整快照不变。

- [ ] **Step 2: 更新 DDC 总览 API 说明**

在英文和中文总览的 Admin/API 章节逐项列出以下 12 个新增 URL：

```text
GET /api/v1/ddc/bizs/page
GET /api/v1/ddc/namespaces/page
GET /api/v1/ddc/envs/page
GET /api/v1/ddc/apps/page
GET /api/v1/ddc/namespace-env-app-bindings/page
GET /api/v1/ddc/configs/page
GET /api/v1/ddc/configs/{id}/versions/page
GET /api/v1/ddc/publish-tasks/page
GET /api/v1/ddc/instances/page
GET /api/v1/ddc/cache/check/page
GET /api/v1/ddc/registry/services/page
GET /api/v1/ddc/registry/instances/page
```

并说明请求与响应契约：

```text
GET /api/v1/ddc/bizs/page?pageNo=1&pageSize=10
success -> PageResultRecord { records, page }
failure -> existing ResultRecord error envelope
legacy list/catalog/snapshot endpoints remain available
```

不得回写历史 specs/plans。

- [ ] **Step 3: 运行完整 Backend 定向 suite**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest='DdcAdminPageSupportTest,Ddc*ControllerTest,Ddc*PagingRepositoryTest,DdcPublishTaskQueryServiceTest,DdcRegistryAdminPageServiceTest,DdcConfigServiceTest,DdcCacheServiceTest,DdcInstanceAdminServiceTest,DdcNamespaceEnvAppBindingServiceTest,DdcManagementRpcProviderTest,DdcConfigRpcProviderTest,DdcRegistryRpcProviderTest,DdcAdminSecurityIntegrationTest,DdcAdminSecurityPropertiesTest,DdcAdminTransportSecurityValidatorTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: Maven exit 0，0 failures，0 errors。

- [ ] **Step 4: 编译受影响 Backend reactor**

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am -DskipTests compile
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 运行完整 Frontend verification**

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm test
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run typecheck
npm run lint
env VITE_IDP_ISSUER=http://127.0.0.1:18120 \
  VITE_IDP_CLIENT_ID=ddc-admin-web \
  VITE_IDP_AUDIENCE=ddc-admin \
  npm run build
```

Expected: Vitest 0 failures；typecheck、lint、build exit 0。

- [ ] **Step 6: 运行契约与 UI 残留扫描**

```bash
page_route_count=$(rg -n '@GetMapping\("[^\"]*/page"\)' \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller \
  | wc -l | tr -d ' ')
test "$page_route_count" = "12"

! rg -n 'Page(Query|ResultRecord)' \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main

! rg -n 'window\.confirm|pagination=\{\{ pageSize: 10|ddcApi<[^>]+\[\]>.*/page|import.*\bmessage\b.*from .antd.' \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src

! rg -n 'findAll\(\)\.stream\(\).*toVO|subList\(' \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller
```

Expected: 12 routes；三个否定扫描均无匹配。Registry Admin Page Service 中允许且只允许 `DdcAdminPageSupport.slice(...)`。

- [ ] **Step 7: 审查工作区和提交 README**

```bash
git diff --check
git status --short
git diff -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.zh-CN.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/README.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/README.zh-CN.md
```

确认 diff 只描述已经实现的契约，然后：

```bash
git add -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.zh-CN.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/README.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/README.zh-CN.md
git commit -m "docs(ddc): document admin page queries"
```

- [ ] **Step 8: 最终提交审计**

```bash
git status --short --branch
git log --oneline --decorate -12
```

Expected: 工作区无本任务未提交文件；Task 1–12 各有一个职责单一的提交；未启动任何项目或浏览器进程。

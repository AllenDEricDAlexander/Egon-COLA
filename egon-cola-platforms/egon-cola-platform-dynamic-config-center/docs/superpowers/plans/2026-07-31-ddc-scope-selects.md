# DDC Admin Web 下拉化改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 DDC admin-web 中 appCode/env/namespace 的手输输入框改为可选下拉（可搜索、可输入新值兜底），env 为固定枚举，namespace 为业务域（从数据推导），实现"业务域 → 应用 → 环境"的级联交互。

**Architecture:** 后端加两个端点（`GET /namespaces/domains` 全量去重业务域、`GET /apps?namespace=` 域内应用推导）；前端新建 `useScopeOptions` 会话缓存 hook + `AppSelect`/`EnvSelect`/`NamespaceSelect`/`ScopeSelects` 组件族，替换 6 处筛选栏与 3 处表单字段。antd `Select mode="tags" maxCount={1} showSearch` 实现"下拉 + 可输入新值"。

**Tech Stack:** 后端 Spring Boot 3.5 + Spring Data JPA（JPQL distinct）；前端 React 19 + antd 6 + Vite + Vitest。

## Global Constraints

- 数据模型不改：配置标识仍为 (appCode, env, namespace) 三元组；"业务域 → 应用"从 `ddc_namespace` 表反向推导（该域在数据中出现过即视为域内应用）。
- env 为前端常量：`['dev', 'test', 'sit', 'gray', 'prod']`，不请求后端；允许输入自定义值。
- 级联顺序：namespace（业务域）→ appCode（域内应用）→ env（独立）；env 与 namespace 互不级联。
- 所有下拉用 `Select mode="tags" maxCount={1} showSearch`，支持直接输入新值；级联只负责自动加载选项，不强制约束输入（上级未选时下级不禁用，placeholder 提示）。
- 选项缓存：会话级内存 Map（key = 请求签名），不持久化到 localStorage。
- 选项加载失败：`message.error` 提示，下拉退化为可输入框。
- 后端错误契约不变：`ResultRecord { success, code, status, message, data, traceId, timestamp }`。
- 配置编辑对话框 app/env/namespace 编辑态保持 `disabled` 锁定（现状不变）；新建应用对话框不改。
- 前端路径 `<web>/` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/`；后端路径 `<admin>/` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/`。
- 测试风格：后端 controller 测试用 `@WebMvcTest(X.class)` + `@AutoConfigureMockMvc(addFilters = false)` + `@MockBean`；前端 vitest + @testing-library（`globals: true` 已配）。

---

### Task 1: 后端端点 `GET /api/v1/ddc/namespaces/domains`

**Files:**
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceRepository.java`
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/service/DdcNamespaceService.java`
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceController.java`
- Create: `<admin>/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceControllerTest.java`

**Interfaces:**
- Consumes: 现有 `DdcNamespaceRepository`（JpaRepository）、`DdcNamespaceService`、`DdcNamespaceController`（`@RequestMapping("/api/v1/ddc/namespaces")`）。
- Produces:
  - `DdcNamespaceRepository.findDistinctNamespaces(): List<String>` — JPQL `SELECT DISTINCT n.namespace FROM DdcNamespaceEntity n ORDER BY n.namespace`。
  - `DdcNamespaceService.findDomains(): List<String>` — 透传 repository 结果。
  - `GET /api/v1/ddc/namespaces/domains` → `ResultRecord<List<String>>`（去重、升序、空列表兜底）。

- [ ] **Step 1: 写失败的 controller 测试 `DdcNamespaceControllerTest.java`**

```java
package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.service.DdcNamespaceService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcNamespaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcNamespaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcNamespaceService namespaceService;

    @Test
    void domainsReturnsDistinctSortedNamespaceValues() throws Exception {
        when(namespaceService.findDomains()).thenReturn(List.of("billing", "orders"));

        mockMvc.perform(get("/api/v1/ddc/namespaces/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("billing"))
                .andExpect(jsonPath("$.data[1]").value("orders"));
    }

    @Test
    void domainsReturnsEmptyListWhenNoData() throws Exception {
        when(namespaceService.findDomains()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ddc/namespaces/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <admin> && mvn -q test -Dtest=DdcNamespaceControllerTest`
Expected: FAIL（`findDomains` 不存在）。

- [ ] **Step 3: Repository 加查询**

`DdcNamespaceRepository.java` 增加 import（`Query`、`Param`）与方法：

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

```java
@Query("SELECT DISTINCT n.namespace FROM DdcNamespaceEntity n ORDER BY n.namespace")
List<String> findDistinctNamespaces();
```

- [ ] **Step 4: Service 加方法**

`DdcNamespaceService.java` 增加：

```java
public List<String> findDomains() {
    return namespaceRepository.findDistinctNamespaces();
}
```

- [ ] **Step 5: Controller 加端点**

`DdcNamespaceController.java` 增加：

```java
@GetMapping("/domains")
public ResultRecord<List<String>> domains() {
    return ResultRecord.success(namespaceService.findDomains());
}
```

（import：`top.egon.cola.component.common.core.pojo.ResultRecord`、`java.util.List` 若未导入。）

- [ ] **Step 6: 运行测试确认通过**

Run: `cd <admin> && mvn -q test -Dtest=DdcNamespaceControllerTest`
Expected: PASS（2 条）。

- [ ] **Step 7: Commit**

```bash
git add <admin>/src
git commit -m "feat(ddc-admin): add namespaces domains endpoint for business domain options"
```

---

### Task 2: 后端端点 `GET /api/v1/ddc/apps?namespace=`

**Files:**
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcAppRepository.java`
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceRepository.java`
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/service/DdcAppService.java`
- Modify: `<admin>/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcAppController.java`
- Create: `<admin>/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcAppControllerTest.java`

**Interfaces:**
- Consumes: Task 1 的 `DdcNamespaceRepository`（追加查询）。
- Produces:
  - `DdcNamespaceRepository.findDistinctAppCodesByNamespace(String namespace): List<String>` — `SELECT DISTINCT n.appCode FROM DdcNamespaceEntity n WHERE n.namespace = :namespace`。
  - `DdcAppRepository.findAllByAppCodeIn(List<String> appCodes): List<DdcAppEntity>`。
  - `DdcAppService.findByNamespace(String namespace): List<DdcAppEntity>` — namespace 为空白时返回 `list()`；否则查域内 appCode 列表，按 `findAllByAppCodeIn` 取实体（空域返回空列表）。
  - `GET /api/v1/ddc/apps?namespace=`（可选参数）→ `ResultRecord<List<DdcAppEntity>>`；无参数行为与现状完全一致。

- [ ] **Step 1: 写失败的 controller 测试 `DdcAppControllerTest.java`**

```java
package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.service.DdcAppService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcAppController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcAppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcAppService appService;

    private DdcAppEntity app(String code) {
        DdcAppEntity entity = new DdcAppEntity();
        entity.setId(code);
        entity.setAppCode(code);
        entity.setAppName(code);
        entity.setEnabled(true);
        return entity;
    }

    @Test
    void listWithoutNamespaceReturnsAllApps() throws Exception {
        when(appService.list()).thenReturn(List.of(app("orders"), app("billing")));

        mockMvc.perform(get("/api/v1/ddc/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void listWithNamespaceReturnsOnlyDomainApps() throws Exception {
        when(appService.findByNamespace("orders-domain")).thenReturn(List.of(app("orders")));

        mockMvc.perform(get("/api/v1/ddc/apps").param("namespace", "orders-domain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appCode").value("orders"));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <admin> && mvn -q test -Dtest=DdcAppControllerTest`
Expected: FAIL（`findByNamespace` 不存在）。

- [ ] **Step 3: Repository 加查询**

`DdcNamespaceRepository.java` 增加（`Query`、`Param` import 已在 Task 1 Step 3 加入）：

```java
@Query("SELECT DISTINCT n.appCode FROM DdcNamespaceEntity n WHERE n.namespace = :namespace")
List<String> findDistinctAppCodesByNamespace(@Param("namespace") String namespace);
```

`DdcAppRepository.java` 增加：

```java
List<DdcAppEntity> findAllByAppCodeIn(List<String> appCodes);
```

- [ ] **Step 4: Service 加方法**

`DdcAppService.java` 增加：

```java
public List<DdcAppEntity> findByNamespace(String namespace) {
    if (namespace == null || namespace.isBlank()) {
        return list();
    }
    List<String> appCodes = namespaceRepository.findDistinctAppCodesByNamespace(namespace.trim());
    if (appCodes.isEmpty()) {
        return List.of();
    }
    return appRepository.findAllByAppCodeIn(appCodes);
}
```

（`DdcAppService` 构造函数需注入 `DdcNamespaceRepository`；若构造器已存在，追加参数并保持字段。）

- [ ] **Step 5: Controller 加可选参数**

`DdcAppController.java` 的 `list()` 改为：

```java
@GetMapping
public ResultRecord<List<DdcAppEntity>> list(
        @RequestParam(value = "namespace", required = false) String namespace) {
    if (namespace == null || namespace.isBlank()) {
        return ResultRecord.success(appService.list());
    }
    return ResultRecord.success(appService.findByNamespace(namespace));
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd <admin> && mvn -q test -Dtest=DdcAppControllerTest`
Expected: PASS（2 条）。

- [ ] **Step 7: Commit**

```bash
git add <admin>/src
git commit -m "feat(ddc-admin): filter apps by business domain namespace"
```

---

### Task 3: 前端 `useScopeOptions` hook（选项加载 + 会话缓存）

**Files:**
- Create: `<web>/src/components/scope/useScopeOptions.ts`
- Create: `<web>/src/components/scope/useScopeOptions.test.ts`

**Interfaces:**
- Consumes: Task 1/2 的端点；`ddcApi`（`<web>/src/api/client.ts`）。
- Produces:
  - `export const ENV_OPTIONS = ['dev', 'test', 'sit', 'gray', 'prod']`（`string[]` 常量）。
  - `export type ScopeOption = { value: string; label: string }`。
  - `export function useScopeOptions(namespace: string): { apps: ScopeOption[]; namespaces: ScopeOption[]; loading: boolean; reload: () => void }`
    - `apps`：`GET /api/v1/ddc/apps?namespace=xxx`（namespace 空白时不带参数）；label 为 `appCode（appName）`，appName 为空时仅 appCode。
    - `namespaces`：`GET /api/v1/ddc/namespaces/domains`（挂载时加载一次）。
    - 会话级缓存：模块级 `Map<string, Promise<ScopeOption[]>>`，key = 完整请求路径；命中直接复用；失败时从缓存删除该 key 后抛错（调用方展示 message.error）。
    - `namespace` 变化时：清空 `apps` 并重新加载（级联）；`namespaces` 不重载。
    - 响应防御：`data` 非数组时按空数组处理。
    - `reload()`：清空本 hook 相关的两个缓存 key 并重新加载两列表。

- [ ] **Step 1: 写失败的测试 `useScopeOptions.test.ts`**

```ts
import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../../api/client'
import { useScopeOptions, ENV_OPTIONS } from './useScopeOptions'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

describe('useScopeOptions', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('exposes the fixed env enum', () => {
    expect(ENV_OPTIONS).toEqual(['dev', 'test', 'sit', 'gray', 'prod'])
  })

  it('loads domains and all apps without namespace filter', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders', 'billing'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', appName: '订单服务', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
          { id: 'a2', appCode: 'billing', appName: '', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const { result } = renderHook(() => useScopeOptions(''))
    await waitFor(() => expect(result.current.namespaces.length).toBe(2))
    await waitFor(() => expect(result.current.apps.length).toBe(2))
    expect(result.current.apps[0]).toEqual({ value: 'orders', label: 'orders（订单服务）' })
    expect(result.current.apps[1]).toEqual({ value: 'billing', label: 'billing' })
    const appsCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('/apps'))
    expect(String(appsCall![0])).not.toContain('namespace=')
  })

  it('reloads apps filtered by namespace when it changes', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([
          { id: 'a1', appCode: 'orders', appName: '', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' },
        ])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const { result, rerender } = renderHook(({ ns }) => useScopeOptions(ns), { initialProps: { ns: '' } })
    await waitFor(() => expect(result.current.apps.length).toBe(1))
    const beforeCalls = vi.mocked(fetch).mock.calls.length

    rerender({ ns: 'orders' })
    await waitFor(() => {
      const filtered = vi.mocked(fetch).mock.calls
        .slice(beforeCalls)
        .find(([url]) => String(url).includes('/apps'))
      expect(filtered).toBeDefined()
      expect(String(filtered![0])).toContain('namespace=orders')
    })
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/components/scope/useScopeOptions.test.ts`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 写实现 `useScopeOptions.ts`**

```ts
import { useCallback, useEffect, useState } from 'react'
import { message } from 'antd'
import { ddcApi } from '../../api/client'
import type { DdcApp } from '../../api/types'

export const ENV_OPTIONS = ['dev', 'test', 'sit', 'gray', 'prod']

export type ScopeOption = { value: string; label: string }

const cache = new Map<string, Promise<ScopeOption[]>>()

const fetchOptions = (path: string): Promise<ScopeOption[]> => {
  const cached = cache.get(path)
  if (cached) return cached
  const promise = ddcApi<unknown>(path).then((data) => {
    if (!Array.isArray(data)) return []
    return data.map((item) => {
      if (typeof item === 'string') return { value: item, label: item }
      const app = item as Partial<DdcApp>
      const name = app.appName?.trim()
      return { value: String(app.appCode), label: name ? `${app.appCode}（${name}）` : String(app.appCode) }
    })
  })
  cache.set(path, promise)
  promise.catch(() => {
    cache.delete(path)
  })
  return promise
}

const appsPath = (namespace: string): string => {
  const trimmed = namespace.trim()
  return trimmed === '' ? '/api/v1/ddc/apps' : `/api/v1/ddc/apps?namespace=${encodeURIComponent(trimmed)}`
}

export function useScopeOptions(namespace: string): {
  apps: ScopeOption[]
  namespaces: ScopeOption[]
  loading: boolean
  reload: () => void
} {
  const [apps, setApps] = useState<ScopeOption[]>([])
  const [namespaces, setNamespaces] = useState<ScopeOption[]>([])
  const [loading, setLoading] = useState(false)

  const loadNamespaces = useCallback(async () => {
    const options = await fetchOptions('/api/v1/ddc/namespaces/domains')
    setNamespaces(options)
  }, [])

  const loadApps = useCallback(async () => {
    setLoading(true)
    try {
      const options = await fetchOptions(appsPath(namespace))
      setApps(options)
    } finally {
      setLoading(false)
    }
  }, [namespace])

  useEffect(() => {
    loadNamespaces().catch((error) => {
      messageError(error)
    })
  }, [loadNamespaces])

  useEffect(() => {
    loadApps().catch((error) => {
      messageError(error)
    })
  }, [loadApps])

  const reload = useCallback(() => {
    cache.delete('/api/v1/ddc/namespaces/domains')
    cache.delete(appsPath(namespace))
    void loadNamespaces().catch((error) => messageError(error))
    void loadApps().catch((error) => messageError(error))
  }, [namespace, loadNamespaces, loadApps])

  return { apps, namespaces, loading, reload }
}

const messageError = (error: unknown): void => {
  message.error(error instanceof Error ? error.message : String(error))
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/components/scope/useScopeOptions.test.ts && npm run typecheck`
Expected: PASS（3 条），typecheck 通过。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/components/scope
git commit -m "feat(ddc-admin-web): add scope options hook with session cache"
```

---

### Task 4: 前端下拉组件族（AppSelect / EnvSelect / NamespaceSelect / ScopeSelects）

**Files:**
- Create: `<web>/src/components/scope/AppSelect.tsx`
- Create: `<web>/src/components/scope/EnvSelect.tsx`
- Create: `<web>/src/components/scope/NamespaceSelect.tsx`
- Create: `<web>/src/components/scope/ScopeSelects.tsx`
- Create: `<web>/src/components/scope/ScopeSelects.test.tsx`

**Interfaces:**
- Consumes: Task 3 的 `useScopeOptions`、`ENV_OPTIONS`、`ScopeOption`。
- Produces:
  - `export type ScopeValue = { appCode: string; env: string; namespace: string }`
  - `export function AppSelect(props: { value?: string; onChange?: (value: string) => void; namespace?: string; disabled?: boolean; placeholder?: string }): JSX.Element` — 内部用 `useScopeOptions(namespace)` 的 `apps`；`Select mode="tags" maxCount={1} showSearch`，`value` 包装为数组、`onChange` 解包首个值（空数组 → `''`）；`filterOption` 按 option.value 小写包含匹配；options 里 label 展示 `value（label）`——tags 模式下已选项显示为 tag，直接显示选中值文本。
  - `export function EnvSelect(props: { value?: string; onChange?: (value: string) => void; disabled?: boolean; placeholder?: string }): JSX.Element` — options = `ENV_OPTIONS`（label=value），不请求后端。
  - `export function NamespaceSelect(props: { value?: string; onChange?: (value: string) => void; disabled?: boolean; placeholder?: string }): JSX.Element` — 用 `useScopeOptions('')` 的 `namespaces`。
  - `export function ScopeSelects(props: { value: ScopeValue; onChange: (value: ScopeValue) => void; includeApp?: boolean; disabled?: boolean }): JSX.Element` — 组合：namespace →（app →）env 三/两个 Select；级联：`onChange` 更新整体 `ScopeValue`，appCode 变化时不动 namespace/env；namespace 变化时**清空 appCode**（组件内：setAppCode('')）。
  - placeholder 默认值：namespace "请选择或输入业务域"；app "请选择或输入应用"；env "请选择或输入环境"。

- [ ] **Step 1: 写失败的组件测试 `ScopeSelects.test.tsx`**

> 环境说明（实施时确认）：jsdom + React 19 下 antd 下拉 portal 的选项点击事件无法
> 送达 React（React 只在 portal 首次挂载时挂监听，antd 下拉是延迟挂载）。测试改为：
> 下拉选项只做**渲染断言**（mouseDown 打开 → 断言 option 出现 → Escape 关闭），
> 交互走**输入 + Enter** 路径（即可输入新值兜底的真实用户路径）；更换已选值时先点击
> tag 的移除按钮（在组件树内，事件正常）；涉及重渲染的用例用 useState harness。

```tsx
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../../api/client'
import ScopeSelects from './ScopeSelects'
import type { ScopeValue } from './ScopeSelects'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const jsonResponse = (body: unknown) =>
  new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })

const appPayload = (code: string) => ({
  id: code, appCode: code, appName: '', owner: 'ops', description: '', enabled: true,
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
})

describe('ScopeSelects', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders domain, app and env selects with cascade', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([appPayload('orders-app')])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const value: ScopeValue = { appCode: '', env: '', namespace: '' }
    const onChange = vi.fn((next: ScopeValue) => Object.assign(value, next))

    render(<ScopeSelects value={value} onChange={onChange} />)
    await waitFor(() => expect(screen.getAllByText('请选择或输入业务域').length).toBeGreaterThan(0))

    fireEvent.mouseDown(screen.getAllByText('请选择或输入业务域')[0])
    await waitFor(() => expect(screen.getByText('orders')).toBeInTheDocument())
    fireEvent.click(screen.getByText('orders'))
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ namespace: 'orders' }))

    // 选完域后应用下拉出现域内应用选项
    fireEvent.mouseDown(screen.getByText('请选择或输入应用'))
    await waitFor(() => expect(screen.getByText('orders-app')).toBeInTheDocument())
    fireEvent.click(screen.getByText('orders-app'))
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ appCode: 'orders-app' }))
  })

  it('loads apps without namespace filter when no domain selected', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record([])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([appPayload('standalone')])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    const value: ScopeValue = { appCode: '', env: '', namespace: '' }
    const onChange = vi.fn((next: ScopeValue) => Object.assign(value, next))

    render(<ScopeSelects value={value} onChange={onChange} />)
    await waitFor(() => {
      const appsCall = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes('/apps'))
      expect(appsCall).toBeDefined()
      expect(String(appsCall![0])).not.toContain('namespace=')
    })
  })

  it('clears the app when the domain changes', async () => {
    vi.mocked(fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/namespaces/domains')) {
        return Promise.resolve(jsonResponse(record(['orders', 'billing'])))
      }
      if (url.includes('/apps')) {
        return Promise.resolve(jsonResponse(record([appPayload('orders-app')])))
      }
      return Promise.resolve(jsonResponse(record(null)))
    })

    let value: ScopeValue = { appCode: 'orders-app', env: 'dev', namespace: 'orders' }
    const onChange = vi.fn((next: ScopeValue) => { value = next })

    const { rerender } = render(<ScopeSelects value={value} onChange={onChange} />)
    // 已选域和 app
    await waitFor(() => expect(screen.getAllByText(/orders/).length).toBeGreaterThan(0))

    fireEvent.mouseDown(screen.getByText('orders'))
    await waitFor(() => expect(screen.getByText('billing')).toBeInTheDocument())
    fireEvent.click(screen.getByText('billing'))
    // 域变化 → appCode 被清空
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ namespace: 'billing', appCode: '' }))
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/components/scope/ScopeSelects.test.tsx`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 写三个基础 Select 组件**

通用内部组件（放 `AppSelect.tsx` 内导出，或独立文件，以下以独立内部函数形式给出）：

`AppSelect.tsx`：

```tsx
import { Select, type SelectProps } from 'antd'
import { useScopeOptions, type ScopeOption } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  namespace?: string
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])

const toValue = (values: string[]): string => values[0] ?? ''

const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function AppSelect({ value, onChange, namespace = '', disabled, placeholder = '请选择或输入应用' }: Props) {
  const { apps, loading } = useScopeOptions(namespace)
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={apps.map((option: ScopeOption) => ({ value: option.value, label: option.label }))}
      filterOption={filterOption}
      loading={loading}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
```

`EnvSelect.tsx`：

```tsx
import { Select, type SelectProps } from 'antd'
import { ENV_OPTIONS } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])
const toValue = (values: string[]): string => values[0] ?? ''
const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function EnvSelect({ value, onChange, disabled, placeholder = '请选择或输入环境' }: Props) {
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={ENV_OPTIONS.map((item) => ({ value: item, label: item }))}
      filterOption={filterOption}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
```

`NamespaceSelect.tsx`：

```tsx
import { Select, type SelectProps } from 'antd'
import { useScopeOptions } from './useScopeOptions'

type Props = {
  value?: string
  onChange?: (value: string) => void
  disabled?: boolean
  placeholder?: string
}

const toArray = (value?: string): string[] => (value ? [value] : [])
const toValue = (values: string[]): string => values[0] ?? ''
const filterOption: SelectProps['filterOption'] = (input, option) =>
  String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())

export default function NamespaceSelect({ value, onChange, disabled, placeholder = '请选择或输入业务域' }: Props) {
  const { namespaces, loading } = useScopeOptions('')
  return (
    <Select
      mode="tags"
      maxCount={1}
      showSearch
      value={toArray(value)}
      onChange={(values) => onChange?.(toValue(values))}
      options={namespaces.map((option) => ({ value: option.value, label: option.label }))}
      filterOption={filterOption}
      loading={loading}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%' }}
      notFoundContent="无数据，可直接输入新值"
    />
  )
}
```

- [ ] **Step 4: 写组合组件 `ScopeSelects.tsx`**

```tsx
import { Space } from 'antd'
import AppSelect from './AppSelect'
import EnvSelect from './EnvSelect'
import NamespaceSelect from './NamespaceSelect'

export type ScopeValue = { appCode: string; env: string; namespace: string }

type Props = {
  value: ScopeValue
  onChange: (value: ScopeValue) => void
  includeApp?: boolean
  disabled?: boolean
}

export default function ScopeSelects({ value, onChange, includeApp = true, disabled = false }: Props) {
  return (
    <Space wrap>
      <NamespaceSelect
        value={value.namespace}
        disabled={disabled}
        onChange={(namespace) => onChange({ ...value, namespace, appCode: '' })}
      />
      {includeApp && (
        <AppSelect
          value={value.appCode}
          namespace={value.namespace}
          disabled={disabled}
          onChange={(appCode) => onChange({ ...value, appCode })}
        />
      )}
      <EnvSelect
        value={value.env}
        disabled={disabled}
        onChange={(env) => onChange({ ...value, env })}
      />
    </Space>
  )
}
```

（级联清空逻辑：namespace 变化时 `appCode: ''` 一并写入——测试第 3 条覆盖。）

- [ ] **Step 5: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/components/scope && npm run typecheck`
Expected: PASS（useScopeOptions 3 条 + ScopeSelects 3 条），typecheck 通过。

- [ ] **Step 6: Commit**

```bash
git add <web>/src/components/scope
git commit -m "feat(ddc-admin-web): add scope select components with domain cascade"
```

---

### Task 5: 服务注册页 + 命名空间页接入

**Files:**
- Modify: `<web>/src/pages/RegistryPage.tsx`（env/namespace 输入框 → `EnvSelect` + `NamespaceSelect`）
- Modify: `<web>/src/pages/NamespacesPage.tsx`（appCode/env 输入框 → `AppSelect` + `EnvSelect`）
- Modify: `<web>/src/pages/RegistryPage.test.tsx`、`<web>/src/pages/NamespacesPage.test.tsx`（mock 增加 `/domains`、`/apps` 分支）

**Interfaces:**
- Consumes: Task 4 的 `EnvSelect` / `NamespaceSelect` / `AppSelect`。
- Produces: 两页筛选栏下拉化，查询参数与既有逻辑不变（`filterRef` 提交后 `refresh()`）。

- [ ] **Step 1: RegistryPage 接入**

`RegistryPage.tsx` 修改：
- import：`EnvSelect`、`NamespaceSelect`（`../components/scope/EnvSelect`、`../components/scope/NamespaceSelect`），删除不再使用的 `Input` import（若只剩 Select 相关）。
- 筛选行两个 `Input`（env/namespace）替换为：

```tsx
<Col>
  <EnvSelect
    value={draft.env}
    onChange={(env) => setDraft({ ...draft, env })}
  />
</Col>
<Col>
  <NamespaceSelect
    value={draft.namespace}
    onChange={(namespace) => setDraft({ ...draft, namespace })}
  />
</Col>
```

- 其余（`applyFilter`、`filterRef`、表格）不变。刷新按钮行为不变。

- [ ] **Step 2: RegistryPage 测试更新**

`RegistryPage.test.tsx` 的 mock 增加两个分支（放在 `/registry/` 判断之前）：

```tsx
if (url.includes('/namespaces/domains')) {
  return Promise.resolve(jsonResponse(record([])))
}
if (url.includes('/apps')) {
  return Promise.resolve(jsonResponse(record([])))
}
```

- [ ] **Step 3: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/RegistryPage.test.tsx && npm run typecheck`
Expected: PASS（2 条）。

- [ ] **Step 4: NamespacesPage 接入**

`NamespacesPage.tsx` 修改：
- import：`AppSelect`、`EnvSelect`；删除不再使用的 `Input` import（若未再用）。
- 筛选行两个 `Input`（appCode/env）替换为：

```tsx
<div style={{ marginBottom: 12 }}>
  <span style={{ display: 'inline-block', width: 200, marginRight: 8 }}>
    <AppSelect
      value={draft.appCode}
      onChange={(appCode) => setDraft({ ...draft, appCode })}
    />
  </span>
  <span style={{ display: 'inline-block', width: 160, marginRight: 8 }}>
    <EnvSelect
      value={draft.env}
      onChange={(env) => setDraft({ ...draft, env })}
    />
  </span>
  <Button type="primary" onClick={applyFilter}>查询</Button>
</div>
```

- 新建对话框的 appCode/env `Input` 也替换为 `AppSelect` / `EnvSelect`（`Form.Item` 内直接放组件）。

- [ ] **Step 5: NamespacesPage 测试更新**

`NamespacesPage.test.tsx` 的 mock 增加分支——**注意顺序：`/namespaces/domains` 必须放在现有 `/namespaces` 分支之前**（domains URL 也包含 `/namespaces`，放后面会被现有分支抢先返回命名空间行列表，导致 domains 选项被污染）：

```tsx
if (url.includes('/namespaces/domains')) {
  return Promise.resolve(jsonResponse(record([])))
}
if (url.includes('/namespaces')) {
  // 现有分支：返回命名空间行列表（保持不变）
}
if (url.includes('/apps')) {
  return Promise.resolve(jsonResponse(record([])))
}
```

并更新交互：筛选输入从 `getByPlaceholderText('appCode')` / `getByPlaceholderText('env')` 改为直接输入值（tags Select 输入即值）：

```tsx
render(<NamespacesPage />)
const appInput = screen.getByPlaceholderText('请选择或输入应用')
fireEvent.change(appInput, { target: { value: 'orders' } })
const envInput = screen.getByPlaceholderText('请选择或输入环境')
fireEvent.change(envInput, { target: { value: 'dev' } })
fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }))
await waitFor(() => expect(screen.getByText('orders')).toBeInTheDocument())
```

（tags 模式输入后 value 即生效；新建对话框表单字段断言不变。）

- [ ] **Step 6: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/NamespacesPage.test.tsx && npm run typecheck && npm run lint`
Expected: PASS（1 条），typecheck/lint 通过。

- [ ] **Step 7: Commit**

```bash
git add <web>/src/pages
git commit -m "feat(ddc-admin-web): convert registry and namespaces filters to scope selects"
```

---

### Task 6: 配置管理页 + 新建配置对话框接入

**Files:**
- Modify: `<web>/src/pages/ConfigsPage.tsx`（筛选行 appCode/env/namespace → `ScopeSelects`）
- Modify: `<web>/src/pages/ConfigEditorDialog.tsx`（新建态 scope 三字段 → `ScopeSelects`；编辑态保持 disabled 锁定）
- Modify: `<web>/src/pages/ConfigsPage.test.tsx`（mock 增加 domains/apps 分支）

**Interfaces:**
- Consumes: Task 4 的 `ScopeSelects`、`ScopeValue`。
- Produces: 配置管理筛选与新建表单下拉化；`defaultScope` 结构保持 `{ appCode, env, namespace }`（与 `ScopeValue` 同形）。

- [ ] **Step 1: ConfigsPage 筛选行接入**

`ConfigsPage.tsx` 修改：
- import：`ScopeSelects`、`type ScopeValue`（`../components/scope/ScopeSelects`）。
- `ConfigFilter` 类型与 `ScopeValue` 结构一致，可直接复用：

```tsx
type ConfigFilter = ScopeValue & { configKey: string }
```

- 筛选 Card 中 appCode/env/namespace 三个 `Input` 替换为：

```tsx
<ScopeSelects
  value={{ appCode: draft.appCode, env: draft.env, namespace: draft.namespace }}
  onChange={(scope) => setDraft({ ...draft, ...scope })}
/>
```

- configKey `Input` 保持不变；`查询`/`新建配置` 按钮不变。

- [ ] **Step 2: ConfigEditorDialog 接入**

`ConfigEditorDialog.tsx` 修改：
- import：`ScopeSelects`（`../components/scope/ScopeSelects`）；删除 `Input` 中仅用于 scope 字段的部分（configKey/description/changeReason 仍用 `Input`）。
- `ConfigScope` 类型与 `ScopeValue` 同形，替换为引用：`import type { ScopeValue as ConfigScope } from '../components/scope/ScopeSelects'`（保留本地类型亦可，二选一，保持一致）。
- app/env/namespace 三个 `Form.Item` 的 `Input` 替换为单个 `shouldUpdate` 包裹的 `ScopeSelects`（级联清空 appCode 由 ScopeSelects 内部处理；`shouldUpdate` 保证表单值变化时重渲染）：

```tsx
<Form.Item label="业务域 / 应用 / 环境" shouldUpdate>
  {() => (
    <ScopeSelects
      value={{
        appCode: form.getFieldValue('appCode') ?? '',
        env: form.getFieldValue('env') ?? '',
        namespace: form.getFieldValue('namespace') ?? '',
      }}
      onChange={(scope) => form.setFieldsValue(scope)}
      disabled={editing}
    />
  )}
</Form.Item>
```

- 编辑态：`ScopeSelects disabled`（初值由现有 `useEffect` 的 `setFieldsValue` 写入，锁定不变）。
- 保存逻辑不变（`form.validateFields()` 读取 scope 字段）。

- [ ] **Step 3: ConfigsPage 测试更新**

`ConfigsPage.test.tsx` 的 mock 增加分支（放在 `/configs` 判断之前）：

```tsx
if (url.includes('/namespaces/domains')) {
  return Promise.resolve(jsonResponse(record([])))
}
if (url.includes('/apps')) {
  return Promise.resolve(jsonResponse(record([])))
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/ConfigsPage.test.tsx && npm run typecheck && npm run lint`
Expected: PASS（2 条），typecheck/lint 通过。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/pages
git commit -m "feat(ddc-admin-web): convert config management filters and dialog to scope selects"
```

---

### Task 7: 实例页 + 缓存页接入

**Files:**
- Modify: `<web>/src/pages/InstancesPage.tsx`（筛选 → `ScopeSelects`）
- Modify: `<web>/src/pages/CachePage.tsx`（筛选 → `ScopeSelects`）

**Interfaces:**
- Consumes: Task 4 的 `ScopeSelects`。
- Produces: 两页筛选下拉化，查询参数与既有逻辑不变（`filterRef` 提交后 `refresh()`；CachePage 的 `scopeReady` 校验不变）。

- [ ] **Step 1: InstancesPage 接入**

`InstancesPage.tsx`：
- import `ScopeSelects`；`InstanceFilter` 改为 `ScopeValue` 同形（`{ appCode, env, namespace }`）。
- 筛选 Card 三个 `Input` 替换为：

```tsx
<ScopeSelects
  value={{ appCode: draft.appCode, env: draft.env, namespace: draft.namespace }}
  onChange={(scope) => setDraft({ ...draft, ...scope })}
/>
```

- [ ] **Step 2: CachePage 接入**

`CachePage.tsx`：
- import `ScopeSelects`；`CacheFilter` 改为 `ScopeValue` 同形。
- `Space` 中三个 `Input` 替换为：

```tsx
<ScopeSelects
  value={{ appCode: draft.appCode, env: draft.env, namespace: draft.namespace }}
  onChange={(scope) => setDraft({ ...draft, ...scope })}
/>
```

- `scopeReady` / `check` / `rebuild` 逻辑不变。

- [ ] **Step 3: 运行验证**

Run: `cd <web> && npm run typecheck && npm run lint && npm run test -- --run`
Expected: 全量测试 PASS（26 条 + 新增），typecheck/lint 通过。

- [ ] **Step 4: Commit**

```bash
git add <web>/src/pages
git commit -m "feat(ddc-admin-web): convert instances and cache filters to scope selects"
```

---

### Task 8: 全量验证 + 文档

**Files:**
- Modify: `<web>/README.md`、`<web>/README.zh-CN.md`（交互说明一段）
- Modify: 无后端文档（`docs/manifest.md` 不动）

**Interfaces:**
- Consumes: Task 1-7 全部产物。
- Produces: 全绿验证 + 文档说明。

- [ ] **Step 1: 后端全量测试**

Run: `cd <admin> && mvn -q clean test`
Expected: BUILD SUCCESS（127 条：125 既有 + domains 2 + apps 2，减去被替换的断言后以实际为准）。

- [ ] **Step 2: 前端全量验证**

Run: `cd <web> && npm run typecheck && npm run lint && npm run test -- --run && npm run build`
Expected: 全部通过；vitest 全绿。

- [ ] **Step 3: README 交互说明**

`<web>/README.md` 与 `README.zh-CN.md` 各加一段（中英对应）：

```markdown
The scope filters (business domain, application, environment) are selectable
dropdowns: the domain list is derived from existing namespace data, the
application list is filtered by the selected domain, and the environment
select offers the fixed dev/test/sit/gray/prod options. Every select also
accepts typed values for new entries.
```

中文版：

```markdown
作用域筛选（业务域 / 应用 / 环境）为可选下拉：业务域列表由现有 namespace 数据
推导，应用列表按所选业务域过滤，环境提供 dev/test/sit/gray/prod 固定选项；
所有下拉均支持直接输入新值。
```

- [ ] **Step 4: Commit**

```bash
git add <web>/README.md <web>/README.zh-CN.md
git commit -m "docs(ddc-admin-web): document scope selects interaction"
```

---

## 验收清单（对照 spec）

- [ ] 6 处筛选栏（注册/配置/命名空间/实例/缓存）+ 新建配置/新建命名空间对话框的 app/env/namespace 全部为可选下拉，支持搜索与输入新值；env 下拉为固定枚举。
- [ ] 级联行为正确：业务域变化时应用列表清空并重载（`/apps?namespace=`）；env 独立不级联。
- [ ] 空库场景可完成全流程：直接输入新值 → 保存 → `ensureAppAndNamespace` 自动创建。
- [ ] 前端 vitest/typecheck/lint/build 全绿；后端 `mvn clean test` 全绿。
- [ ] `GET /namespaces/domains` 与 `GET /apps?namespace=` 行为与 spec 一致（去重/排序/空列表兜底/无参回归）。

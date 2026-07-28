# DDC Config Editor Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 DDC Admin 自动识别 JSON、YAML、TOML、TXT，并只编辑 Gateway 发布外壳中的真实规则内容。

**Architecture:** 新增原生 ESM 配置展示 Adapter，以纯函数完成格式识别、编辑模型生成和持久化值重建。现有 `app.js` 只负责 DOM 与 API，把 Gateway checksum、canonical JSON 等细节留在可独立测试的模块内。

**Tech Stack:** 原生 HTML/CSS/JavaScript ESM、Node `node:test`、Web Crypto、Java 21、Spring Boot、JUnit 5、Maven。

## Global Constraints

- 当前 `main` 分支 inline execution，不创建子代理或 worktree。
- 不使用浏览器，不重启当前 DDC、Gateway 或 Provider 服务。
- 不新增依赖，不修改数据库或既有 Flyway migration。
- 保留两个既有未跟踪文件，不纳入提交。
- `valueType` 保持 DDC 运行时类型语义，文件类型只作为自动识别的展示属性。
- 只解包结构完整的 Gateway INLINE activation，不按字段名删除普通业务 JSON 内容。

---

### Task 1: 配置展示 Adapter

**Files:**
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/config-format.mjs`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/js/ddc-admin/config-format.test.mjs`

**Interfaces:**
- Produces: `detectConfigFormat(config): 'JSON' | 'YAML' | 'TOML' | 'TXT'`。
- Produces: `prepareConfigEditor(config): { format, content, adapter, originalValue, notice }`。
- Produces: `serializeConfigEditor(editor, content): Promise<string>`。

- [x] **Step 1: 写文件类型识别失败测试**

使用字面量配置分别断言 `.yaml`、`.toml`、`.txt`、`valueType=JSON`；另用
`{"data":{"version":1},"metadata":{"owner":"ops"}}` 断言普通 JSON 完整保留。

- [x] **Step 2: 验证 RED**

Run:

```bash
node --test egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/js/ddc-admin/config-format.test.mjs
```

Expected: FAIL，`config-format.mjs` 尚不存在。

- [x] **Step 3: 写 Gateway 外壳失败测试**

固定 INLINE activation fixture，断言 `prepareConfigEditor` 只输出缩进后的：

```json
{
  "env": "dev",
  "routes": []
}
```

将内容改为 `env=prod` 后调用 `serializeConfigEditor`，断言：

```text
ruleContentSha256 = a270803a31aceb109ad9e65bd4993c02049e2717798dc3be95b462b81c47167b
artifactSha256 = 784dc9c7bb589bdb5ab542f6170f0fa5751b89ad6d0a1035b2dd7d5902889303
totalSize = 295
```

并断言 releaseId、generatedAt、activationSchemaVersion 和 chunks 保留。

- [x] **Step 4: 写最小实现**

实现扩展名/内容识别、严格 Gateway envelope 匹配、JSON 缩进、canonical JSON、SHA-256 和
Gateway snapshot/activation 重建。普通 YAML、TOML、TXT 原样往返；普通 JSON 只做解析校验和缩进，
不抽取 `data`、`metadata` 等业务字段。

- [x] **Step 5: 验证 GREEN**

重复 Step 2 命令，Expected: 所有 Adapter 行为测试 PASS。

### Task 2: DDC Admin 页面接入

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/app.js`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/index.html`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/styles.css`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/web/DdcAdminWebResourceTest.java`

**Interfaces:**
- Consumes: Task 1 的 `detectConfigFormat`、`prepareConfigEditor`、`serializeConfigEditor`。
- Produces: 配置列表的“值类型/文件类型/配置内容”列和带自动格式提示的编辑弹窗。

- [x] **Step 1: 写资源打包失败测试**

扩展 `DdcAdminWebResourceTest`，读取 `static/ddc-admin/config-format.mjs`，验证新 ESM 是 classpath
资源。先运行测试，Expected: FAIL，因为资源尚不存在。

- [x] **Step 2: 接入列表展示**

`renderConfigs` 使用 Adapter 生成内容预览，显示独立文件类型 badge，不再预览 Gateway 发布元数据。

- [x] **Step 3: 接入编辑与保存**

`openConfigDialog` 保存当前 editor model 并填入净化内容；`saveConfig` 在 POST/PUT 前 await
`serializeConfigEditor`。键名、值类型、内容输入变化时刷新自动类型与说明。

- [x] **Step 4: 更新页面结构和样式**

新增只读文件类型、自动识别提示和更高的等宽配置编辑区；保持当前响应式布局和视觉变量。

- [x] **Step 5: 验证页面资源测试**

Run:

```bash
./mvnw -B -ntp -pl :egon-cola-component-dynamic-config-center-admin -am \
  -Dtest=DdcAdminWebResourceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

### Task 3: 完整验证和提交

**Files:**
- Verify all files from Tasks 1-2 and this plan only.

**Interfaces:**
- Consumes: Adapter、静态页面和 classpath 资源测试。
- Produces: 可由用户自行重启 DDC Admin 后验收的提交。

- [x] **Step 1: 运行 JavaScript 全部测试**

```bash
node --test egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/js/ddc-admin/*.test.mjs
```

- [x] **Step 2: 运行 DDC Admin 模块测试与打包**

```bash
./mvnw -B -ntp -pl :egon-cola-component-dynamic-config-center-admin -am test
./mvnw -B -ntp -pl :egon-cola-component-dynamic-config-center-admin -am -DskipTests package
```

- [x] **Step 3: 检查变更并提交**

```bash
git diff --check
git status --short
git add docs/superpowers/plans/2026-07-28-ddc-config-editor-format.md \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/js/ddc-admin/config-format.test.mjs \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/web/DdcAdminWebResourceTest.java
git commit -m "feat: improve ddc config editing"
```

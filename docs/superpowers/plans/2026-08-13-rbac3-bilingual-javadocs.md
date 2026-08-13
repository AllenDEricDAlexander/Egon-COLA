# RBAC3 Bilingual JavaDoc Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 RBAC3 Admin、Gateway Adapter、Starter 的 `src/main/java` 全部包、类、字段、构造器和方法补齐中英双语 JavaDoc，并为每个 Java 包增加 `package-info.java`。

**Architecture:** 仅增加源代码文档，不改变业务逻辑、API 签名、注解、导入或运行时行为。保留已有有价值的 JavaDoc，在缺失处补充职责、用法和语义说明；包级说明按模块和分层职责描述。

**Tech Stack:** Java 21、Maven reactor、JDK `javadoc`/`doclint`、仓库现有 Spring Boot modules。

## Global Constraints

- 只修改 `egon-cola-platform-rbac3-admin`、`egon-cola-platform-rbac3-gateway-adapter`、`egon-cola-platform-rbac3-starter` 的 `src/main/java` 文档内容。
- 覆盖 private/protected/public/static 成员、构造器、嵌套类、接口方法、枚举常量、record 组件和 compact constructor。
- 每个 JavaDoc 同时包含中文和英文；内容说明职责、用法或语义，不只重复名称。
- 每个实际 Java package 创建一个 `package-info.java`，package 声明与目录保持一致。
- 不启动服务、不查询数据库、不修改既有 Flyway migration，不覆盖工作区其他未提交改动。

---

### Task 1: 建立覆盖范围和文档生成边界

**Files:**
- Inspect: `egon-cola-platforms/egon-cola-platform-rbac3/*/src/main/java/**/*.java`
- Inspect: `egon-cola-platforms/egon-cola-platform-rbac3/{pom.xml,egon-cola-platform-rbac3-admin/pom.xml,egon-cola-platform-rbac3-gateway-adapter/pom.xml,egon-cola-platform-rbac3-starter/pom.xml}`

- [ ] **Step 1: Enumerate packages and source declarations**

Run:

```bash
for module in admin gateway-adapter starter; do
  root="egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-${module}/src/main/java"
  rg --files "$root" --glob '*.java' | sort
  rg '^package ' "$root" --glob '*.java' | sort -u
done
```

Expected baseline: 181 Admin Java files in 61 packages, 10 Gateway Adapter files in 3 packages, and 22 Starter files in 9 packages, with no existing `package-info.java` files.

- [ ] **Step 2: Record existing documentation conventions**

Inspect representative application, domain, infrastructure, configuration, security, record, enum, and nested-type declarations. Keep the existing `/** ... */` style and only add documentation text or missing documentation blocks.

- [ ] **Step 3: Verify the worktree boundary**

Run `git status --short --untracked-files=all` and retain every pre-existing change outside the three requested `src/main/java` trees.

### Task 2: Add package-level bilingual documentation

**Files:**
- Create: one `package-info.java` in each of the 73 discovered Java package directories under the three requested modules.

- [ ] **Step 1: Create package declarations with bilingual responsibility and usage**

Each new file follows this shape, replacing the package name and responsibility text with the directory's actual role:

```java
/**
 * 说明本包在 RBAC3 中承担的边界、主要类型和使用方式。
 * Describes this package's RBAC3 boundary, main types, and usage.
 *
 * <p>包内类型协同完成对应的控制面、网关适配或业务应用授权职责。
 * Types in this package collaborate to provide the corresponding control-plane,
 * Gateway-adapter, or business-application authorization responsibility.</p>
 */
package top.egon.cola.platform.rbac3.example;
```

- [ ] **Step 2: Check package coverage**

Verify that every package declaration has exactly one matching `package-info.java`, and that every package-info file contains Chinese and English prose.

### Task 3: Complete Admin declaration documentation

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/**/*.java`

- [ ] **Step 1: Document every class-like declaration**

For each top-level and nested class, interface, enum, record, exception, configuration type, and application entry point, document its responsibility, lifecycle/ownership, and normal usage in Chinese and English. Preserve existing precise behavior descriptions.

- [ ] **Step 2: Document every field and record component**

Describe what each field stores or represents, its unit/identity/version meaning where applicable, and how callers use it. Include constants, injected dependencies, state holders, enum constants, and record components.

- [ ] **Step 3: Document every constructor and method**

Describe the operation and side effects in Chinese and English; add `@param`, `@return`, and `@throws` entries where the signature has corresponding values or declared failures. Include private helpers and compact constructors rather than limiting coverage to public APIs.

### Task 4: Complete Gateway Adapter and Starter declaration documentation

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-gateway-adapter/src/main/java/**/*.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/**/*.java`

- [ ] **Step 1: Document Gateway Adapter declarations**

Cover auto-configuration, runtime integration, credential extraction/sanitization, JWT verification, authentication providers, and nested result/exception types with bilingual responsibility and usage descriptions.

- [ ] **Step 2: Document Starter declarations**

Cover auto-configuration, authorization service/aspect, HTTP client, cache/single-flight loader, event/manifest extension points, security filter/token, runtime properties, controller/error handling, nested records, enum constants, constructors, fields, and methods.

- [ ] **Step 3: Confirm no behavior changes**

Review the diff for documentation-only changes: no imports, annotations, signatures, modifiers, executable statements, constants, or resource files may change.

### Task 5: Audit and validate

**Files:**
- Inspect: all changed Java files and generated `package-info.java` files.

- [ ] **Step 1: Run a declaration/documentation coverage audit**

Parse the Java sources using the JDK compiler tree API or an equivalent source-aware audit and assert that every package, class-like declaration, field/record component, enum constant, constructor, and method has an immediately associated JavaDoc containing both a Chinese character and English prose.

- [ ] **Step 2: Run focused Maven verification**

From `egon-cola-platforms/egon-cola-platform-rbac3`, run:

```bash
mvn -pl egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3-gateway-adapter,egon-cola-platform-rbac3-admin -am -DskipTests compile
```

Then run the module tests if compilation succeeds:

```bash
mvn -pl egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3-gateway-adapter,egon-cola-platform-rbac3-admin -am test
```

- [ ] **Step 3: Run strict Javadoc validation where the reactor classpath is available**

Run the repository's Javadoc goal or a focused `javadoc -quiet -private -Xdoclint:all` invocation against the changed modules. If an installed stale artifact causes an unrelated missing-type error, use the current reactor `target/classes` and report the exact boundary instead of weakening documentation coverage.

- [ ] **Step 4: Recheck scope and summarize evidence**

Run `git diff --stat` and `git diff --check`; confirm that only the three requested source trees plus the local plan file changed, no service was started, and all failed or unavailable checks are reported explicitly.

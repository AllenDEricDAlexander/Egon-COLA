# Common Component Enterprise Toolkit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `egon-cola-component-common` as a pure Java enterprise common toolkit with new result, query, exception, model, trace, and utility contracts.

**Architecture:** Replace the old COLA-style API with direct enterprise Java contracts and thin facade utilities. Keep common free of Spring, Jakarta Validation, DTP, and business dependencies; use mature libraries only where they reduce low-value custom code.

**Tech Stack:** Java 21, Maven, JUnit 5, SLF4J MDC, Apache Commons Lang, Apache Commons Collections, Commons Codec, Jackson.

---

## File Structure

### Maven

- Modify: `egon-cola-components/pom.xml`
  - Add version properties for common dependencies not already managed by Spring Boot BOM.
  - Add dependency management for `commons-collections4` and `commons-codec` if needed.
- Modify: `egon-cola-components/egon-cola-component-common/pom.xml`
  - Add dependencies: `slf4j-api`, `commons-lang3`, `commons-collections4`, `commons-codec`, `jackson-databind`, `jackson-datatype-jsr310`.

### Remove Old API

- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BaseException.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BizException.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SysException.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/ClientObject.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Command.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/DTO.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Query.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageQuery.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageResponse.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/MultiResponse.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Response.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/SingleResponse.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation/Assert.java`

### New Core Contracts

- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/ErrorCodes.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BusinessException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SystemException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseModel.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseRequest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseEntity.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/AuditableModel.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/query/PageQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Result.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/PageResult.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/trace/TraceContext.java`

### New Utility Facades

- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Strings.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Collections2.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Dates.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Jsons.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Ids.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/CodeEnum.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Enums.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Masking.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Crypto.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Trees.java`

### Tests

- Replace: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/CommonComponentBoundaryTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/result/ResultTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/result/PageResultTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/query/PageQueryTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/exception/CommonExceptionTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/model/BaseModelTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/trace/TraceContextTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/util/CommonUtilsTest.java`

### Archetype Alignment

- Inspect with `rg "top\.egon\.cola\.component\.common\.|component.common.(page|result|validation|model|exception)" egon-cola-archetypes egon-cola-components --glob '*.java' --glob '!**/target/**'`.
- Modify only files that fail compilation because they reference deleted common classes.
- Do not replace archetype-local `${package}.common.exceptions.BizException` or `${package}.facade.dto.PageResponse` unless compilation proves it is necessary.

---

### Task 1: Add Common Dependencies

**Files:**
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-common/pom.xml`

- [ ] **Step 1: Add dependency-management properties**

In `egon-cola-components/pom.xml`, add these properties near the existing dependency version properties:

```xml
<commons.collections4.version>4.5.0</commons.collections4.version>
<commons.codec.version>1.18.0</commons.codec.version>
```

- [ ] **Step 2: Add managed dependencies when not covered by Spring BOM**

In `egon-cola-components/pom.xml`, add these entries inside `<dependencyManagement><dependencies>`:

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-collections4</artifactId>
    <version>${commons.collections4.version}</version>
</dependency>
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>${commons.codec.version}</version>
</dependency>
```

Do not add explicit versions for `slf4j-api`, `commons-lang3`, `jackson-databind`, or `jackson-datatype-jsr310`; these are managed by the imported Spring Boot BOM.

- [ ] **Step 3: Add common module dependencies**

In `egon-cola-components/egon-cola-component-common/pom.xml`, add:

```xml
<dependencies>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-collections4</artifactId>
    </dependency>
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
</dependencies>
```

- [ ] **Step 4: Run dependency resolution**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am dependency:tree -Dincludes=org.slf4j:slf4j-api,org.apache.commons:commons-lang3,org.apache.commons:commons-collections4,commons-codec:commons-codec,com.fasterxml.jackson.core:jackson-databind,com.fasterxml.jackson.datatype:jackson-datatype-jsr310
```

Expected: Maven resolves the common module dependency tree without errors and shows the six dependencies.

- [ ] **Step 5: Commit dependencies**

Run:

```bash
git add egon-cola-components/pom.xml egon-cola-components/egon-cola-component-common/pom.xml
git commit -m "build: add common toolkit dependencies"
```

---

### Task 2: Replace Exception Contracts

**Files:**
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BaseException.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BizException.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SysException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/ErrorCodes.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BusinessException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SystemException.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/exception/CommonExceptionTest.java`

- [ ] **Step 1: Write exception tests**

Create `CommonExceptionTest.java`:

```java
package top.egon.cola.component.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommonExceptionTest {

    @Test
    void businessExceptionKeepsCodeMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("bad input");

        BusinessException exception = new BusinessException("ORDER_NOT_FOUND", "订单不存在", cause);

        assertEquals("ORDER_NOT_FOUND", exception.getCode());
        assertEquals("订单不存在", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void businessExceptionUsesDefaultCode() {
        BusinessException exception = new BusinessException("业务处理失败");

        assertEquals(ErrorCodes.BUSINESS_ERROR, exception.getCode());
        assertEquals("业务处理失败", exception.getMessage());
    }

    @Test
    void systemExceptionUsesDefaultCode() {
        SystemException exception = new SystemException("系统处理失败");

        assertEquals(ErrorCodes.SYSTEM_ERROR, exception.getCode());
        assertEquals("系统处理失败", exception.getMessage());
    }
}
```

- [ ] **Step 2: Run the new test and verify it fails**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonExceptionTest test
```

Expected: FAIL because `BusinessException`, `SystemException`, and `ErrorCodes` do not exist yet.

- [ ] **Step 3: Delete old exception classes**

Remove these files:

```bash
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BaseException.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BizException.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SysException.java
```

- [ ] **Step 4: Create ErrorCodes**

Create `ErrorCodes.java`:

```java
package top.egon.cola.component.common.exception;

/**
 * common 组件默认错误码。
 */
public final class ErrorCodes {

    public static final String SUCCESS = "SUCCESS";

    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";

    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    public static final String PARAM_ERROR = "PARAM_ERROR";

    private ErrorCodes() {
    }
}
```

- [ ] **Step 5: Create BusinessException**

Create `BusinessException.java`:

```java
package top.egon.cola.component.common.exception;

/**
 * 业务可预期异常，调用方可根据错误码进行业务处理。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public BusinessException(String message) {
        this(ErrorCodes.BUSINESS_ERROR, message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        this(ErrorCodes.BUSINESS_ERROR, message, cause);
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

- [ ] **Step 6: Create SystemException**

Create `SystemException.java`:

```java
package top.egon.cola.component.common.exception;

/**
 * 系统不可预期异常，通常需要记录日志并排查基础设施或程序错误。
 */
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public SystemException(String message) {
        this(ErrorCodes.SYSTEM_ERROR, message);
    }

    public SystemException(String code, String message) {
        super(message);
        this.code = code;
    }

    public SystemException(String message, Throwable cause) {
        this(ErrorCodes.SYSTEM_ERROR, message, cause);
    }

    public SystemException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

- [ ] **Step 7: Run exception tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonExceptionTest test
```

Expected: PASS.

- [ ] **Step 8: Commit exception contracts**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/exception/CommonExceptionTest.java
git commit -m "feat: replace common exception contracts"
```

---

### Task 3: Add Trace And Result Contracts

**Files:**
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/MultiResponse.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Response.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/SingleResponse.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/trace/TraceContext.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Result.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/trace/TraceContextTest.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/result/ResultTest.java`

- [ ] **Step 1: Write TraceContext test**

Create `TraceContextTest.java`:

```java
package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void getsAndSetsTraceIdByStandardKey() {
        TraceContext.setTraceId("trace-001");

        assertEquals("trace-001", TraceContext.getTraceId());
        assertEquals("trace-001", MDC.get("traceId"));
    }

    @Test
    void doesNotReadAliasKeys() {
        MDC.put("trace_id", "trace-alias");

        assertNull(TraceContext.getTraceId());
    }

    @Test
    void clearsTraceIdOnly() {
        MDC.put("traceId", "trace-001");
        MDC.put("other", "value");

        TraceContext.clearTraceId();

        assertNull(MDC.get("traceId"));
        assertEquals("value", MDC.get("other"));
    }
}
```

- [ ] **Step 2: Write Result test**

Create `ResultTest.java`:

```java
package top.egon.cola.component.common.result;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.exception.BusinessException;
import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.trace.TraceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void successResultCarriesTraceId() {
        TraceContext.setTraceId("trace-001");

        Result<String> result = Result.success("ok");

        assertTrue(result.isSuccess());
        assertEquals(ErrorCodes.SUCCESS, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("ok", result.getData());
        assertEquals("trace-001", result.getTraceId());
    }

    @Test
    void failureResultCarriesTraceId() {
        TraceContext.setTraceId("trace-002");

        Result<String> result = Result.failure("ORDER_NOT_FOUND", "订单不存在");

        assertFalse(result.isSuccess());
        assertEquals("ORDER_NOT_FOUND", result.getCode());
        assertEquals("订单不存在", result.getMessage());
        assertNull(result.getData());
        assertEquals("trace-002", result.getTraceId());
    }

    @Test
    void failureFromBusinessExceptionUsesExceptionCode() {
        Result<Void> result = Result.failure(new BusinessException("ORDER_NOT_FOUND", "订单不存在"));

        assertFalse(result.isSuccess());
        assertEquals("ORDER_NOT_FOUND", result.getCode());
        assertEquals("订单不存在", result.getMessage());
    }
}
```

- [ ] **Step 3: Run tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=TraceContextTest,ResultTest test
```

Expected: FAIL because `TraceContext` and new `Result` do not exist yet.

- [ ] **Step 4: Delete old result classes**

Run:

```bash
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/MultiResponse.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Response.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/SingleResponse.java
```

- [ ] **Step 5: Create TraceContext**

Create `TraceContext.java`:

```java
package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

/**
 * traceId 上下文工具，统一使用 MDC key：traceId。
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(TRACE_ID);
            return;
        }
        MDC.put(TRACE_ID, traceId);
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }
}
```

- [ ] **Step 6: Create Result**

Create `Result.java`:

```java
package top.egon.cola.component.common.result;

import top.egon.cola.component.common.exception.BusinessException;
import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.exception.SystemException;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serializable;

/**
 * 通用单对象返回结果，自动携带当前 MDC 中的 traceId。
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;

    private String code;

    private String message;

    private T data;

    private String traceId;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.code = ErrorCodes.SUCCESS;
        result.message = "success";
        result.data = data;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public static <T> Result<T> failure(String code, String message) {
        Result<T> result = new Result<>();
        result.success = false;
        result.code = code;
        result.message = message;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public static <T> Result<T> failure(Throwable throwable) {
        if (throwable instanceof BusinessException exception) {
            return failure(exception.getCode(), exception.getMessage());
        }
        if (throwable instanceof SystemException exception) {
            return failure(exception.getCode(), exception.getMessage());
        }
        String message = throwable == null ? "system error" : throwable.getMessage();
        return failure(ErrorCodes.SYSTEM_ERROR, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
```

- [ ] **Step 7: Run trace and result tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=TraceContextTest,ResultTest test
```

Expected: PASS.

- [ ] **Step 8: Commit trace and result contracts**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/trace egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/result/ResultTest.java egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/trace/TraceContextTest.java
git commit -m "feat: add trace aware result contract"
```

---

### Task 4: Replace Model Contracts

**Files:**
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/ClientObject.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Command.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/DTO.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Query.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseModel.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseRequest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/BaseEntity.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/AuditableModel.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/model/BaseModelTest.java`

- [ ] **Step 1: Write model tests**

Create `BaseModelTest.java`:

```java
package top.egon.cola.component.common.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseModelTest {

    @Test
    void baseModelStoresExtensionFields() {
        BaseModel model = new BaseModel();

        model.putExtension("source", "api");

        assertEquals("api", model.getExtension("source"));
        assertEquals(Map.of("source", "api"), model.getExtensions());
    }

    @Test
    void baseModelIgnoresBlankExtensionKey() {
        BaseModel model = new BaseModel();

        model.putExtension(" ", "api");

        assertNull(model.getExtension(" "));
    }

    @Test
    void auditableModelKeepsAuditFields() {
        AuditableModel model = new AuditableModel();
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 12, 0);

        model.setCreatedAt(now);
        model.setCreatedBy("egon");
        model.setUpdatedAt(now.plusHours(1));
        model.setUpdatedBy("mario");
        model.setDeleted(false);

        assertEquals(now, model.getCreatedAt());
        assertEquals("egon", model.getCreatedBy());
        assertEquals(now.plusHours(1), model.getUpdatedAt());
        assertEquals("mario", model.getUpdatedBy());
        assertEquals(false, model.getDeleted());
    }

    @Test
    void baseEntityKeepsId() {
        BaseEntity<Long> entity = new BaseEntity<>();

        entity.setId(1L);

        assertEquals(1L, entity.getId());
    }
}
```

- [ ] **Step 2: Run model test and verify it fails**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=BaseModelTest test
```

Expected: FAIL because new model classes do not exist yet.

- [ ] **Step 3: Delete old model classes**

Run:

```bash
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/ClientObject.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Command.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/DTO.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Query.java
```

- [ ] **Step 4: Create BaseModel**

Create `BaseModel.java`:

```java
package top.egon.cola.component.common.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * common 基础模型，提供序列化和扩展字段能力。
 */
public class BaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Object> extensions = new LinkedHashMap<>();

    public Object getExtension(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return extensions.get(key);
    }

    public void putExtension(String key, Object value) {
        if (key == null || key.isBlank()) {
            return;
        }
        extensions.put(key, value);
    }

    public Map<String, Object> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extensions);
    }
}
```

- [ ] **Step 5: Create BaseRequest and BaseQuery**

Create `BaseRequest.java`:

```java
package top.egon.cola.component.common.model;

/**
 * 请求对象基础类，用于承载跨层传递的输入参数。
 */
public class BaseRequest extends BaseModel {

    private static final long serialVersionUID = 1L;
}
```

Create `BaseQuery.java`:

```java
package top.egon.cola.component.common.model;

/**
 * 查询请求基础类，用于承载查询类输入参数。
 */
public class BaseQuery extends BaseRequest {

    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 6: Create BaseEntity**

Create `BaseEntity.java`:

```java
package top.egon.cola.component.common.model;

/**
 * 可选实体基础类，提供通用 id 字段。
 */
public class BaseEntity<ID> extends BaseModel {

    private static final long serialVersionUID = 1L;

    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
}
```

- [ ] **Step 7: Create AuditableModel**

Create `AuditableModel.java`:

```java
package top.egon.cola.component.common.model;

import java.time.LocalDateTime;

/**
 * 可选审计模型，适用于需要记录创建和更新信息的对象。
 */
public class AuditableModel extends BaseModel {

    private static final long serialVersionUID = 1L;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    private Boolean deleted;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
```

- [ ] **Step 8: Run model tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=BaseModelTest test
```

Expected: PASS.

- [ ] **Step 9: Commit model contracts**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/model/BaseModelTest.java
git commit -m "feat: replace common model contracts"
```

---

### Task 5: Add Page Query And Page Result

**Files:**
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageQuery.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageResponse.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/query/PageQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/PageResult.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/query/PageQueryTest.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/result/PageResultTest.java`

- [ ] **Step 1: Write PageQuery test**

Create `PageQueryTest.java`:

```java
package top.egon.cola.component.common.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void normalizesPageValuesAndCalculatesOffset() {
        PageQuery query = new PageQuery();
        query.setPageNo(0);
        query.setPageSize(0);

        assertEquals(1, query.getPageNo());
        assertEquals(1, query.getPageSize());
        assertEquals(0, query.getOffset());
    }

    @Test
    void acceptsOnlyKnownOrderDirections() {
        PageQuery query = new PageQuery();

        query.setOrderDirection("ASC");
        assertEquals(PageQuery.ASC, query.getOrderDirection());

        query.setOrderDirection("bad");
        assertEquals(PageQuery.ASC, query.getOrderDirection());
    }
}
```

- [ ] **Step 2: Write PageResult test**

Create `PageResultTest.java`:

```java
package top.egon.cola.component.common.result;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void buildsPageMetadataAndTraceId() {
        TraceContext.setTraceId("trace-page");

        PageResult<String> result = PageResult.success(List.of("a", "b"), 5, 2, 2);

        assertTrue(result.isSuccess());
        assertEquals(ErrorCodes.SUCCESS, result.getCode());
        assertEquals(List.of("a", "b"), result.getRecords());
        assertEquals(5, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getPages());
        assertTrue(result.isHasPrevious());
        assertTrue(result.isHasNext());
        assertEquals("trace-page", result.getTraceId());
    }

    @Test
    void normalizesEmptyRecordsAndPageValues() {
        PageResult<String> result = PageResult.success(null, 0, 0, 0);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(1, result.getPageNo());
        assertEquals(1, result.getPageSize());
        assertEquals(0, result.getPages());
        assertFalse(result.isHasPrevious());
        assertFalse(result.isHasNext());
    }
}
```

- [ ] **Step 3: Run tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=PageQueryTest,PageResultTest test
```

Expected: FAIL because the new `PageQuery` package and `PageResult` do not exist yet.

- [ ] **Step 4: Delete old page classes**

Run:

```bash
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageQuery.java
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageResponse.java
rmdir egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page
```

- [ ] **Step 5: Create PageQuery**

Create `PageQuery.java`:

```java
package top.egon.cola.component.common.query;

import top.egon.cola.component.common.model.BaseQuery;

/**
 * 分页查询基础参数，不绑定 Bean Validation。
 */
public class PageQuery extends BaseQuery {

    private static final long serialVersionUID = 1L;

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MIN_PAGE_VALUE = 1;

    private int pageNo = MIN_PAGE_VALUE;

    private int pageSize = DEFAULT_PAGE_SIZE;

    private String orderBy;

    private String orderDirection = DESC;

    public int getPageNo() {
        return Math.max(pageNo, MIN_PAGE_VALUE);
    }

    public void setPageNo(int pageNo) {
        this.pageNo = Math.max(pageNo, MIN_PAGE_VALUE);
    }

    public int getPageSize() {
        return Math.max(pageSize, MIN_PAGE_VALUE);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(pageSize, MIN_PAGE_VALUE);
    }

    public int getOffset() {
        return (getPageNo() - 1) * getPageSize();
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        if (ASC.equalsIgnoreCase(orderDirection)) {
            this.orderDirection = ASC;
            return;
        }
        if (DESC.equalsIgnoreCase(orderDirection)) {
            this.orderDirection = DESC;
        }
    }
}
```

- [ ] **Step 6: Create PageResult**

Create `PageResult.java`:

```java
package top.egon.cola.component.common.result;

import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页返回结果，自动计算分页元数据并携带 traceId。
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;

    private String code;

    private String message;

    private List<T> records = new ArrayList<>();

    private long total;

    private int pageNo;

    private int pageSize;

    private long pages;

    private boolean hasNext;

    private boolean hasPrevious;

    private String traceId;

    public static <T> PageResult<T> success(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageResult<T> result = new PageResult<>();
        result.success = true;
        result.code = ErrorCodes.SUCCESS;
        result.message = "success";
        result.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
        result.total = normalizedTotal;
        result.pageNo = normalizedPageNo;
        result.pageSize = normalizedPageSize;
        result.pages = totalPages;
        result.hasPrevious = normalizedPageNo > 1 && totalPages > 0;
        result.hasNext = totalPages > normalizedPageNo;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getPages() {
        return pages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public String getTraceId() {
        return traceId;
    }
}
```

- [ ] **Step 7: Run page tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=PageQueryTest,PageResultTest test
```

Expected: PASS after Task 5 provides `BaseQuery`.

- [ ] **Step 8: Commit page contracts**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/query egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/PageResult.java egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/query/PageQueryTest.java egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/result/PageResultTest.java
git add -u egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page
git commit -m "feat: add common page query and result"
```

---

### Task 6: Add Basic Utility Facades

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Strings.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Collections2.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Dates.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Ids.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/CodeEnum.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Enums.java`
- Test: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/util/CommonUtilsTest.java`

- [ ] **Step 1: Write first utility tests**

Create `CommonUtilsTest.java` with the basic utility section:

```java
package top.egon.cola.component.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonUtilsTest {

    enum Status implements CodeEnum<String> {
        ENABLED("1"),
        DISABLED("0");

        private final String code;

        Status(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }
    }

    @Test
    void stringUtilitiesHandleBlankDefaultAndTruncate() {
        assertTrue(Strings.isBlank(" "));
        assertEquals("fallback", Strings.defaultIfBlank(" ", "fallback"));
        assertEquals("abc", Strings.truncate("abcdef", 3));
        assertEquals("hello", Strings.normalize("  hello  "));
    }

    @Test
    void collectionUtilitiesHandleNullSafely() {
        assertTrue(Collections2.isEmpty(null));
        assertEquals(0, Collections2.size(null));
        assertEquals("a", Collections2.first(List.of("a", "b")));
        assertEquals(List.of("A", "B"), Collections2.map(List.of("a", "b"), String::toUpperCase));
    }

    @Test
    void dateUtilitiesFormatParseAndCalculateDayBounds() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 7, 12, 30, 0);

        assertEquals("2026-07-07 12:30:00", Dates.format(time, "yyyy-MM-dd HH:mm:ss"));
        assertEquals(time, Dates.parseDateTime("2026-07-07 12:30:00", "yyyy-MM-dd HH:mm:ss"));
        assertEquals(LocalDate.of(2026, 7, 7).atStartOfDay(), Dates.startOfDay(LocalDate.of(2026, 7, 7)));
        assertEquals(LocalDateTime.of(2026, 7, 7, 23, 59, 59, 999_999_999), Dates.endOfDay(LocalDate.of(2026, 7, 7)));
    }

    @Test
    void idUtilitiesCreateUuidValues() {
        String uuid = Ids.uuid();
        String simpleUuid = Ids.simpleUuid();

        assertEquals(36, uuid.length());
        assertEquals(32, simpleUuid.length());
        assertNotEquals(uuid, simpleUuid);
    }

    @Test
    void enumUtilitiesFindByNameAndCode() {
        assertEquals(Status.ENABLED, Enums.getByName(Status.class, "ENABLED"));
        assertEquals(Status.DISABLED, Enums.getByCode(Status.class, "0"));
        assertTrue(Enums.containsName(Status.class, "ENABLED"));
        assertFalse(Enums.containsName(Status.class, "UNKNOWN"));
    }
}
```

- [ ] **Step 2: Run utility tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonUtilsTest test
```

Expected: FAIL because utility classes do not exist yet.

- [ ] **Step 3: Create Strings**

Create `Strings.java`:

```java
package top.egon.cola.component.common.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具门面，优先复用 Apache Commons Lang。
 */
public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String value) {
        return StringUtils.isBlank(value);
    }

    public static boolean isNotBlank(String value) {
        return StringUtils.isNotBlank(value);
    }

    public static String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0) {
            return value;
        }
        return StringUtils.truncate(value, maxLength);
    }

    public static boolean equals(String left, String right) {
        return StringUtils.equals(left, right);
    }

    public static boolean equalsIgnoreCase(String left, String right) {
        return StringUtils.equalsIgnoreCase(left, right);
    }
}
```

- [ ] **Step 4: Create Collections2**

Create `Collections2.java`:

```java
package top.egon.cola.component.common.util;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 集合工具门面，提供 null-safe 的高频集合操作。
 */
public final class Collections2 {

    private Collections2() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return CollectionUtils.isEmpty(collection);
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return CollectionUtils.isNotEmpty(collection);
    }

    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    public static <T> T first(List<T> list) {
        return isEmpty(list) ? null : list.get(0);
    }

    public static <T> T last(List<T> list) {
        return isEmpty(list) ? null : list.get(list.size() - 1);
    }

    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        List<R> result = new ArrayList<>(collection.size());
        for (T item : collection) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        for (T item : collection) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static <T> List<T> emptyList() {
        return Collections.emptyList();
    }

    public static <T> Set<T> emptySet() {
        return Collections.emptySet();
    }

    public static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }
}
```

- [ ] **Step 5: Create Dates**

Create `Dates.java`:

```java
package top.egon.cola.component.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具，基于 java.time 统一常用转换和格式化。
 */
public final class Dates {

    private Dates() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parseDateTime(String value, String pattern) {
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 999_999_999);
    }

    public static LocalDateTime fromEpochMillis(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    public static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
```

- [ ] **Step 6: Create Ids**

Create `Ids.java`:

```java
package top.egon.cola.component.common.util;

import java.util.UUID;

/**
 * ID 工具，首批提供基于 JDK UUID 的轻量能力。
 */
public final class Ids {

    private Ids() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String simpleUuid() {
        return uuid().replace("-", "");
    }

    public static String shortUuid() {
        return simpleUuid().substring(0, 16);
    }
}
```

- [ ] **Step 7: Create CodeEnum and Enums**

Create `CodeEnum.java`:

```java
package top.egon.cola.component.common.util;

/**
 * 可选枚举编码约定，业务枚举按需实现。
 */
public interface CodeEnum<C> {

    C getCode();
}
```

Create `Enums.java`:

```java
package top.egon.cola.component.common.util;

import java.util.Objects;

/**
 * 枚举查找工具，支持按名称和可选编码查找。
 */
public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E getByName(Class<E> enumType, String name) {
        if (enumType == null || name == null) {
            return null;
        }
        for (E item : enumType.getEnumConstants()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

    public static <E extends Enum<E>> boolean containsName(Class<E> enumType, String name) {
        return getByName(enumType, name) != null;
    }

    public static <C, E extends Enum<E> & CodeEnum<C>> E getByCode(Class<E> enumType, C code) {
        if (enumType == null) {
            return null;
        }
        for (E item : enumType.getEnumConstants()) {
            if (Objects.equals(item.getCode(), code)) {
                return item;
            }
        }
        return null;
    }
}
```

- [ ] **Step 8: Run basic utility tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonUtilsTest test
```

Expected: PASS for the basic utility tests currently present.

- [ ] **Step 9: Commit basic utilities**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/util/CommonUtilsTest.java
git commit -m "feat: add common basic utility facades"
```

---

### Task 7: Add Json Masking Crypto And Tree Utilities

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/util/CommonUtilsTest.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Jsons.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Masking.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Crypto.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util/Trees.java`

- [ ] **Step 1: Extend CommonUtilsTest**

Append these nested test helper types and tests to `CommonUtilsTest.java`:

```java
    static class UserNode {
        private final Long id;
        private final Long parentId;
        private final String name;
        private List<UserNode> children = List.of();

        UserNode(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }

        Long getId() {
            return id;
        }

        Long getParentId() {
            return parentId;
        }

        String getName() {
            return name;
        }

        List<UserNode> getChildren() {
            return children;
        }

        void setChildren(List<UserNode> children) {
            this.children = children;
        }
    }

    static class JsonUser {
        private String name;
        private int age;

        JsonUser() {
        }

        JsonUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    void jsonUtilitiesSerializeAndDeserialize() {
        String json = Jsons.toJson(new JsonUser("egon", 18));

        JsonUser user = Jsons.fromJson(json, JsonUser.class);
        List<JsonUser> users = Jsons.fromJsonList("[{\"name\":\"egon\",\"age\":18}]", JsonUser.class);

        assertEquals("egon", user.getName());
        assertEquals(18, user.getAge());
        assertEquals("egon", users.get(0).getName());
    }

    @Test
    void maskingUtilitiesMaskSensitiveValues() {
        assertEquals("138****8000", Masking.mobile("13812348000"));
        assertEquals("e***@example.com", Masking.email("egon@example.com"));
        assertEquals("110101********1234", Masking.idCard("110101199001011234"));
        assertEquals("6222***********1234", Masking.bankCard("622202020202021234"));
        assertEquals("张*", Masking.name("张三"));
    }

    @Test
    void cryptoUtilitiesDigestEncodeAndHmac() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Crypto.md5Hex("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Crypto.sha256Hex("abc"));
        assertEquals("YWJj", Crypto.base64Encode("abc"));
        assertEquals("abc", Crypto.base64DecodeToString("YWJj"));
        assertEquals("9c196e32dc0175f86f4b1cb89289d6619de6bee699e4c378e68309ed97a1a6ab", Crypto.hmacSha256Hex("abc", "key"));
    }

    @Test
    void treeUtilitiesBuildTreeFromFlatNodes() {
        List<UserNode> roots = Trees.build(
                List.of(new UserNode(1L, null, "root"), new UserNode(2L, 1L, "child")),
                UserNode::getId,
                UserNode::getParentId,
                UserNode::setChildren
        );

        assertEquals(1, roots.size());
        assertEquals("root", roots.get(0).getName());
        assertEquals("child", roots.get(0).getChildren().get(0).getName());
    }
```

Ensure the imports include only:

```java
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
```

- [ ] **Step 2: Run utility tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonUtilsTest test
```

Expected: FAIL because `Jsons`, `Masking`, `Crypto`, and `Trees` do not exist yet.

- [ ] **Step 3: Create Jsons**

Create `Jsons.java`:

```java
package top.egon.cola.component.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.common.exception.SystemException;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具门面，统一收口 Jackson 调用和异常包装。
 */
public final class Jsons {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private Jsons() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new SystemException("JSON_SERIALIZE_ERROR", "JSON 序列化失败", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new SystemException("JSON_DESERIALIZE_ERROR", "JSON 反序列化失败", e);
        }
    }

    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            throw new SystemException("JSON_DESERIALIZE_ERROR", "JSON 反序列化失败", e);
        }
    }

    public static Map<String, Object> toMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new SystemException("JSON_DESERIALIZE_ERROR", "JSON 反序列化失败", e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {
        return OBJECT_MAPPER.convertValue(value, type);
    }
}
```

- [ ] **Step 4: Create Masking**

Create `Masking.java`:

```java
package top.egon.cola.component.common.util;

/**
 * 敏感信息脱敏工具，提供企业高频字段的默认脱敏规则。
 */
public final class Masking {

    private Masking() {
    }

    public static String mobile(String value) {
        return range(value, 3, 7);
    }

    public static String email(String value) {
        if (Strings.isBlank(value) || !value.contains("@")) {
            return value;
        }
        int atIndex = value.indexOf('@');
        String prefix = value.substring(0, atIndex);
        String suffix = value.substring(atIndex);
        if (prefix.length() <= 1) {
            return "*" + suffix;
        }
        return prefix.charAt(0) + "***" + suffix;
    }

    public static String idCard(String value) {
        return range(value, 6, Math.max(6, value == null ? 0 : value.length() - 4));
    }

    public static String bankCard(String value) {
        return range(value, 4, Math.max(4, value == null ? 0 : value.length() - 4));
    }

    public static String name(String value) {
        if (Strings.isBlank(value) || value.length() == 1) {
            return value;
        }
        return value.charAt(0) + "*".repeat(value.length() - 1);
    }

    public static String range(String value, int startInclusive, int endExclusive) {
        if (Strings.isBlank(value)) {
            return value;
        }
        int start = Math.max(0, startInclusive);
        int end = Math.min(value.length(), Math.max(start, endExclusive));
        if (start >= end) {
            return value;
        }
        return value.substring(0, start) + "*".repeat(end - start) + value.substring(end);
    }
}
```

- [ ] **Step 5: Create Crypto**

Create `Crypto.java`:

```java
package top.egon.cola.component.common.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import top.egon.cola.component.common.exception.SystemException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 加密摘要工具，提供摘要、HMAC 和常用编码能力。
 */
public final class Crypto {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private Crypto() {
    }

    public static String md5Hex(String value) {
        return DigestUtils.md5Hex(value);
    }

    public static String sha256Hex(String value) {
        return DigestUtils.sha256Hex(value);
    }

    public static String hmacSha256Hex(String value, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Hex.encodeHexString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new SystemException("CRYPTO_ERROR", "HMAC-SHA256 计算失败", e);
        }
    }

    public static String base64Encode(String value) {
        return Base64.encodeBase64String(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64DecodeToString(String value) {
        return new String(Base64.decodeBase64(value), StandardCharsets.UTF_8);
    }

    public static String hexEncode(String value) {
        return Hex.encodeHexString(value.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 6: Create Trees**

Create `Trees.java`:

```java
package top.egon.cola.component.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树结构工具，支持将扁平列表组装为树。
 */
public final class Trees {

    private Trees() {
    }

    public static <T, ID> List<T> build(Collection<T> nodes,
                                        Function<T, ID> idGetter,
                                        Function<T, ID> parentIdGetter,
                                        BiConsumer<T, List<T>> childrenSetter) {
        if (Collections2.isEmpty(nodes)) {
            return List.of();
        }
        Map<ID, T> nodeMap = new LinkedHashMap<>();
        Map<ID, List<T>> childrenMap = new LinkedHashMap<>();
        for (T node : nodes) {
            ID id = idGetter.apply(node);
            nodeMap.put(id, node);
            childrenMap.putIfAbsent(id, new ArrayList<>());
        }
        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            ID parentId = parentIdGetter.apply(node);
            if (parentId == null || !nodeMap.containsKey(parentId) || Objects.equals(idGetter.apply(node), parentId)) {
                roots.add(node);
                continue;
            }
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
        }
        for (T node : nodes) {
            childrenSetter.accept(node, childrenMap.getOrDefault(idGetter.apply(node), List.of()));
        }
        return roots;
    }
}
```

- [ ] **Step 7: Run all utility tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonUtilsTest test
```

Expected: PASS.

- [ ] **Step 8: Commit advanced utilities**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/util egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/util/CommonUtilsTest.java
git commit -m "feat: add common enterprise utility facades"
```

---

### Task 8: Remove Validation And Update Boundary Tests

**Files:**
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation/Assert.java`
- Replace: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/CommonComponentBoundaryTest.java`

- [ ] **Step 1: Replace boundary test**

Replace `CommonComponentBoundaryTest.java` with:

```java
package top.egon.cola.component.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonComponentBoundaryTest {

    @Test
    void commonSourceDoesNotImportRuntimeFrameworks() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<String> badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(line -> line.startsWith("import org.springframework.")
                            || line.startsWith("import jakarta.")
                            || line.startsWith("import org.redisson.")
                            || line.startsWith("import redis.")
                            || line.startsWith("import top.egon.cola.component.dtp.")
                            || line.startsWith("import com.alibaba.cola."))
                    .toList();
            assertEquals(List.of(), badImports);
        }
    }

    @Test
    void oldColaStyleApiHasBeenRemoved() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common");
        List<String> oldClassNames;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            oldClassNames = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> List.of(
                            "Response.java",
                            "SingleResponse.java",
                            "MultiResponse.java",
                            "DTO.java",
                            "Command.java",
                            "Query.java",
                            "ClientObject.java",
                            "Assert.java",
                            "BizException.java",
                            "SysException.java",
                            "BaseException.java"
                    ).contains(name))
                    .toList();
        }
        assertTrue(oldClassNames.isEmpty());
    }
}
```

This boundary test deliberately allows `javax.crypto` because JDK crypto is part of the approved design.

- [ ] **Step 2: Delete validation package**

Run:

```bash
rm egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation/Assert.java
rmdir egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation
```

- [ ] **Step 3: Run boundary test**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -Dtest=CommonComponentBoundaryTest test
```

Expected: PASS.

- [ ] **Step 4: Commit boundary cleanup**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/CommonComponentBoundaryTest.java
git add -u egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation
git commit -m "test: enforce common component boundaries"
```

---

### Task 9: Align Internal References And Run Full Common Verification

**Files:**
- Modify only files found by the reference scan that still import deleted common APIs.
- Expected likely scope: common tests only. Archetype local classes generally use `${package}.common...` and should not require changes unless verification proves otherwise.

- [ ] **Step 1: Scan deleted API references**

Run:

```bash
rg -n "top\.egon\.cola\.component\.common\.(page|validation|result\.(Response|SingleResponse|MultiResponse)|model\.(DTO|Command|Query|ClientObject)|exception\.(BaseException|BizException|SysException))|\b(Response|SingleResponse|MultiResponse|DTO|Command|ClientObject|Assert|BizException|SysException|BaseException)\b" egon-cola-components egon-cola-archetypes --glob '*.java' --glob '!**/target/**'
```

Expected: No references to deleted `top.egon.cola.component.common` APIs remain. References to archetype-local `${package}.common.exceptions.BizException`, `${package}.common.response.Response`, or `${package}.facade.dto.PageResponse` may remain and should not be changed solely because the class names match.

- [ ] **Step 2: If a component imports deleted common API, replace narrowly**

Use these mappings only for actual `top.egon.cola.component.common` imports:

```text
top.egon.cola.component.common.result.Response -> top.egon.cola.component.common.result.Result
top.egon.cola.component.common.result.SingleResponse -> top.egon.cola.component.common.result.Result
top.egon.cola.component.common.result.MultiResponse -> top.egon.cola.component.common.result.Result<List<T>>
top.egon.cola.component.common.page.PageResponse -> top.egon.cola.component.common.result.PageResult
top.egon.cola.component.common.page.PageQuery -> top.egon.cola.component.common.query.PageQuery
top.egon.cola.component.common.exception.BizException -> top.egon.cola.component.common.exception.BusinessException
top.egon.cola.component.common.exception.SysException -> top.egon.cola.component.common.exception.SystemException
top.egon.cola.component.common.validation.Assert -> remove and use spring-validation-starter or explicit exception in caller
```

Do not modify files that use archetype placeholders such as `${package}.common.exceptions.BizException` unless the exact import is `top.egon.cola.component.common...`.

- [ ] **Step 3: Run full common tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
```

Expected: PASS.

- [ ] **Step 4: Run compile for components reactor**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am compile
```

Expected: PASS.

- [ ] **Step 5: Commit alignment if any files changed**

If Step 2 changed files, run:

```bash
git add <changed-files>
git commit -m "refactor: align common api references"
```

If no files changed, skip this commit and record that no internal alignment was required.

---

### Task 10: Final Verification And Review

**Files:**
- No planned source changes.

- [ ] **Step 1: Run final common verification**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
```

Expected: PASS.

- [ ] **Step 2: Check dependency boundary in dependency tree**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am dependency:tree | rg "spring-|jakarta|redisson|mybatis|component-dynamic-thread-pool|com.alibaba.cola" || true
```

Expected: No forbidden runtime dependencies appear in the common module dependency tree. JUnit test dependencies may appear only through test scope and are acceptable.

- [ ] **Step 3: Check git diff scope**

Run:

```bash
git status --short
git diff --stat HEAD~10..HEAD
```

Expected: Changes are limited to common module, component POM dependency management, tests, and any proven minimal alignment files.

- [ ] **Step 4: Summarize completion**

Prepare final notes with:

```text
- Subagents used and task ownership, if subagent-driven execution was chosen.
- New common APIs and packages.
- Removed old COLA APIs.
- Dependency additions.
- Validation commands and results.
- Any archetype alignment performed or skipped.
- Known migration risk: external consumers must migrate from old common classes.
```

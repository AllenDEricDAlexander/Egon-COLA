# Common Enterprise Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hard-migrate Egon COLA common contracts to record-first enterprise DTO, model, query, status, and exception contracts.

**Architecture:** Keep `common-core` dependency-free and move JSON contract annotations only into `common-result` and `common-model`. Replace mutable DTO/model/query classes with same-package same-name records, then migrate repository callers through compile feedback rather than compatibility bridges.

**Tech Stack:** Java 21, Maven, JUnit 5, Jackson annotations, Jackson databind for tests, SLF4J MDC.

---

## File Structure

### Core

- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/enums/CodeEnum.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/enums/IntCodeEnum.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/code/ErrorStatus.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/code/CommonStatus.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonException.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonBusinessException.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonSystemException.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonIllegalStateException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonValidationException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonUnauthorizedException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonForbiddenException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonNotFoundException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonRemoteCallException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonConcurrencyException.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/CoreBoundaryTest.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/exception/EgonExceptionTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/code/CommonStatusTest.java`

### Build Dependencies

- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml`

### Result

- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/ResultDto.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/PageResultDto.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/ErrorResultDto.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/ResultModel.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/PageResultModel.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/ErrorResultModel.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/factory/ResultDtos.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/factory/ResultModels.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/factory/ResultDtosTest.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/factory/ResultModelsTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/ResultJsonContractTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/ResultSerializationTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/ResultDependencyBoundaryTest.java`

### Model

- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageMeta.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageModel.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageSlice.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/PageQuery.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/SortQuery.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/TimeRangeQuery.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/request/BaseRequest.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/request/OperatorContext.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/page/PageModelTest.java`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/query/PageQueryTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/ModelJsonContractTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/ModelSerializationTest.java`

### Repository Callers

- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Modify only additional files that fail compilation because they call common record contracts through old getters or setters.

---

### Task 1: Core Status and Exception Contracts

**Files:**
- Modify/Create the core files listed in the Core section.

- [ ] **Step 1: Write failing tests for status uniqueness and new exceptions**

Replace `EgonExceptionTest.java` with:

```java
package top.egon.cola.component.common.core.exception;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonExceptionTest {

    @Test
    void businessExceptionCarriesStatusCodeAndMessage() {
        EgonBusinessException exception = new EgonBusinessException(CommonStatus.BAD_REQUEST);

        assertEquals(CommonStatus.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), exception.getMessage());
        assertFalse(exception.isRetryable());
    }

    @Test
    void remoteCallExceptionCanBeRetryable() {
        RuntimeException cause = new RuntimeException("timeout");

        EgonRemoteCallException exception = new EgonRemoteCallException(CommonStatus.REMOTE_CALL_ERROR, true, cause);

        assertEquals(CommonStatus.REMOTE_CALL_ERROR.getCode(), exception.getCode());
        assertEquals(CommonStatus.REMOTE_CALL_ERROR.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.REMOTE_CALL_ERROR.getMessage(), exception.getMessage());
        assertTrue(exception.isRetryable());
        assertSame(cause, exception.getCause());
    }

    @Test
    void typedExceptionsUseMatchingCommonStatus() {
        assertEquals(CommonStatus.VALIDATION_ERROR.getCode(), new EgonValidationException(CommonStatus.VALIDATION_ERROR).getCode());
        assertEquals(CommonStatus.UNAUTHORIZED.getCode(), new EgonUnauthorizedException(CommonStatus.UNAUTHORIZED).getCode());
        assertEquals(CommonStatus.FORBIDDEN.getCode(), new EgonForbiddenException(CommonStatus.FORBIDDEN).getCode());
        assertEquals(CommonStatus.NOT_FOUND.getCode(), new EgonNotFoundException(CommonStatus.NOT_FOUND).getCode());
        assertEquals(CommonStatus.CONCURRENCY_ERROR.getCode(), new EgonConcurrencyException(CommonStatus.CONCURRENCY_ERROR).getCode());
    }
}
```

Create `CommonStatusTest.java`:

```java
package top.egon.cola.component.common.core.code;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonStatusTest {

    @Test
    void codesAndStatusesAreUnique() {
        Set<Integer> codes = Arrays.stream(CommonStatus.values()).map(CommonStatus::getCode).collect(Collectors.toSet());
        Set<String> statuses = Arrays.stream(CommonStatus.values()).map(CommonStatus::getStatus).collect(Collectors.toSet());

        assertEquals(CommonStatus.values().length, codes.size());
        assertEquals(CommonStatus.values().length, statuses.size());
    }

    @Test
    void successCodeIsZeroAndBusinessErrorUsesBusinessRange() {
        assertEquals(0, CommonStatus.SUCCESS.getCode());
        assertTrue(CommonStatus.SUCCESS.isSuccess());
        assertEquals(600000, CommonStatus.BUSINESS_ERROR.getCode());
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-core test
```

Expected: FAIL because new statuses, `isRetryable()`, and new exception types do not exist yet.

- [ ] **Step 3: Implement core contracts**

Add `IntCodeEnum.java`:

```java
package top.egon.cola.component.common.core.enums;

/**
 * Base contract for enums that expose a stable integer code value.
 */
public interface IntCodeEnum {

    int getCode();
}
```

Update `ErrorStatus.java`:

```java
package top.egon.cola.component.common.core.code;

/**
 * Error status contract used by common result and exception models.
 */
public interface ErrorStatus {

    int getCode();

    String getStatus();

    String getMessage();

    default boolean isSuccess() {
        return getCode() == CommonStatus.SUCCESS.getCode();
    }
}
```

Update `CommonStatus.java` with the new enum values:

```java
package top.egon.cola.component.common.core.code;

/**
 * Common enterprise status definitions shared by Egon COLA components.
 */
public enum CommonStatus implements ErrorStatus {

    SUCCESS(0, "SUCCESS", "success"),

    BAD_REQUEST(400000, "BAD_REQUEST", "bad request"),
    UNAUTHORIZED(401000, "UNAUTHORIZED", "unauthorized"),
    FORBIDDEN(403000, "FORBIDDEN", "forbidden"),
    NOT_FOUND(404000, "NOT_FOUND", "not found"),
    VALIDATION_ERROR(422000, "VALIDATION_ERROR", "validation error"),
    TOO_MANY_REQUESTS(429000, "TOO_MANY_REQUESTS", "too many requests"),
    CONCURRENCY_ERROR(409000, "CONCURRENCY_ERROR", "concurrency error"),

    SYSTEM_ERROR(500000, "SYSTEM_ERROR", "system error"),
    REMOTE_CALL_ERROR(510000, "REMOTE_CALL_ERROR", "remote call error"),
    MIDDLEWARE_ERROR(520000, "MIDDLEWARE_ERROR", "middleware error"),

    BUSINESS_ERROR(600000, "BUSINESS_ERROR", "business error");

    private final int code;
    private final String status;
    private final String message;

    CommonStatus(int code, String status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

Update `EgonException.java` to add `retryable`:

```java
package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Base runtime exception carrying stable enterprise error status fields.
 */
public class EgonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;
    private final String status;
    private final boolean retryable;

    public EgonException(ErrorStatus errorStatus) {
        this(errorStatus, false, null);
    }

    public EgonException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus, false, cause);
    }

    public EgonException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), retryable, cause);
    }

    public EgonException(int code, String status, String message) {
        this(code, status, message, false, null);
    }

    public EgonException(int code, String status, String message, Throwable cause) {
        this(code, status, message, false, cause);
    }

    public EgonException(int code, String status, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
```

Create the new typed exception classes using the same constructor pattern. `EgonRemoteCallException` must include:

```java
public EgonRemoteCallException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
    super(errorStatus, retryable, cause);
}
```

Other new exception classes should include constructors for `ErrorStatus`, `ErrorStatus + Throwable`, and `int code, String status, String message`.

- [ ] **Step 4: Harden core boundary test**

Add forbidden import checks for Jackson and Lombok in `CoreBoundaryTest.java`:

```java
|| line.startsWith("import com.fasterxml.jackson.")
|| line.startsWith("import lombok.")
```

- [ ] **Step 5: Run core tests**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-core test
```

Expected: PASS.

- [ ] **Step 6: Commit core contracts**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-core
git commit -m "feat: harden common core contracts"
```

---

### Task 2: Add Annotation and Test Dependencies

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml`

- [ ] **Step 1: Add common-model dependencies**

Add these dependencies after `egon-cola-component-common-core`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Add common-result dependencies**

Add these dependencies after `egon-cola-component-common-trace`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Resolve dependencies**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-model,egon-cola-component-common-result dependency:tree -Dincludes=com.fasterxml.jackson.core:jackson-annotations,com.fasterxml.jackson.core:jackson-databind
```

Expected: Both modules resolve `jackson-annotations`; `jackson-databind` appears only with test scope.

- [ ] **Step 4: Commit dependency changes**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml
git commit -m "build: add common contract annotation dependencies"
```

---

### Task 3: Result Records and Factories

**Files:**
- Modify all result files listed in the Result section.

- [ ] **Step 1: Write failing result tests**

Update existing result tests to use record accessors:

```java
assertTrue(result.success());
assertEquals(CommonStatus.SUCCESS.getCode(), result.code());
assertEquals(CommonStatus.SUCCESS.getStatus(), result.status());
assertEquals("ok", result.data());
```

Create `ResultJsonContractTest.java` with tests that serialize `ResultDtos.success(null)`, `ResultDtos.failure(new NullPointerException("secret"))`, and `ResultDtos.page(null, 0, 0, 0)` using `new ObjectMapper()`. Assert the serialized maps contain every expected key and do not contain `serialVersionUID`.

Create `ResultSerializationTest.java` with an ObjectOutputStream/ObjectInputStream helper:

```java
@SuppressWarnings("unchecked")
private static <T extends Serializable> T roundTrip(T value) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(value);
    }
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
        return (T) input.readObject();
    }
}
```

Test `ResultDto`, `PageResultDto`, `ResultModel`, and `PageResultModel`.

- [ ] **Step 2: Run result tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-result -am test
```

Expected: FAIL because result classes are still mutable classes and old getters exist in tests.

- [ ] **Step 3: Convert DTO and model classes to records**

Convert `ResultDto<T>`, `PageResultDto<T>`, `ErrorResultDto`, `ResultModel<T>`, `PageResultModel<T>`, and `ErrorResultModel` to records with:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({...})
public record ResultDto<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("code") int code,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("data") T data,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("timestamp") Long timestamp
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;
}
```

For `PageResultDto<T>` and `PageResultModel<T>`, add a canonical constructor:

```java
public PageResultDto {
    records = records == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(records));
}
```

- [ ] **Step 4: Convert factories to constructor-based creation**

`ResultDtos.success(T data)` should return:

```java
return new ResultDto<>(
        true,
        CommonStatus.SUCCESS.getCode(),
        CommonStatus.SUCCESS.getStatus(),
        CommonStatus.SUCCESS.getMessage(),
        data,
        TraceContext.getTraceId(),
        now()
);
```

`ResultDtos.failure(Throwable throwable)` should not expose unknown messages:

```java
if (throwable instanceof EgonException exception) {
    return failure(exception.getCode(), exception.getStatus(), exception.getMessage());
}
return failure(
        CommonStatus.SYSTEM_ERROR.getCode(),
        CommonStatus.SYSTEM_ERROR.getStatus(),
        CommonStatus.SYSTEM_ERROR.getMessage()
);
```

`ResultModels` should use record constructors and should not add trace fields.

- [ ] **Step 5: Run result tests**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-result -am test
```

Expected: PASS.

- [ ] **Step 6: Commit result migration**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-result
git commit -m "refactor: migrate common result contracts to records"
```

---

### Task 4: Model, Query, and Request Records

**Files:**
- Modify all model files listed in the Model section.

- [ ] **Step 1: Write failing model tests**

Update `PageModelTest` and `PageQueryTest` to use record constructors and accessors:

```java
PageQuery query = new PageQuery(3, 20);
assertEquals(3, query.pageNo());
assertEquals(20, query.pageSize());
assertEquals(40, query.offset());
```

Create `ModelJsonContractTest.java` for `PageQuery`, `PageModel`, `SortQuery`, `TimeRangeQuery`, `BaseRequest`, and `OperatorContext`. Assert JSON includes explicit null fields where expected.

Create `ModelSerializationTest.java` using the same round-trip helper from Task 3 and test `PageMeta`, `PageModel`, `PageSlice`, `PageQuery`, `SortQuery`, `TimeRangeQuery`, `BaseRequest`, and `OperatorContext`.

- [ ] **Step 2: Run model tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-model -am test
```

Expected: FAIL because model classes are still mutable classes.

- [ ] **Step 3: Convert model classes to records**

Convert `PageMeta`, `PageModel<T>`, `PageSlice<T>`, `PageQuery`, `SortQuery`, `TimeRangeQuery`, `BaseRequest`, and `OperatorContext` to records. Use the JSON annotation pattern from Task 3.

`PageQuery` must look like:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"pageNo", "pageSize"})
public record PageQuery(
        @JsonProperty("pageNo") int pageNo,
        @JsonProperty("pageSize") int pageSize
) implements Serializable {

    @Serial
    @JsonIgnore
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 500;

    public PageQuery {
        pageNo = Math.max(pageNo, DEFAULT_PAGE_NO);
        pageSize = normalizePageSize(pageSize);
    }

    public static PageQuery defaultPage() {
        return new PageQuery(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE);
    }

    @JsonIgnore
    public int offset() {
        return Math.max((pageNo - 1) * pageSize, 0);
    }

    private static int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
```

`SortQuery` canonical constructor must trim blank values and normalize direction to `ASC` or `DESC`.

- [ ] **Step 4: Run model tests**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-model -am test
```

Expected: PASS.

- [ ] **Step 5: Commit model migration**

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-model
git commit -m "refactor: migrate common model contracts to records"
```

---

### Task 5: Compile-Driven Caller Migration

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Modify additional compilation failures only.

- [ ] **Step 1: Run component compile to reveal caller failures**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components -am test -DskipTests
```

Expected before migration: FAIL on old JavaBean accessors for common records.

- [ ] **Step 2: Update known DDC starter caller**

In `HttpDdcAdminClient.pull()`, replace:

```java
return result == null || result.getData() == null ? Collections.emptyList() : result.getData();
```

with:

```java
return result == null || result.data() == null ? Collections.emptyList() : result.data();
```

- [ ] **Step 3: Fix only compile-reported callers**

For each compile error where a common record accessor is called with old JavaBean style, make the direct mechanical change:

```text
result.getData()      -> result.data()
result.getCode()      -> result.code()
result.getStatus()    -> result.status()
result.getMessage()   -> result.message()
result.getRecords()   -> result.records()
result.getTotal()     -> result.total()
result.getPageNo()    -> result.pageNo()
result.getPageSize()  -> result.pageSize()
result.getPages()     -> result.pages()
result.isSuccess()    -> result.success()
result.isHasNext()    -> result.hasNext()
result.isHasPrevious()-> result.hasPrevious()
```

Do not modify unrelated project-local DTOs that merely happen to have `getStatus()` or `setStatus()`.

- [ ] **Step 4: Run component compile again**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components -am test -DskipTests
```

Expected: PASS compilation.

- [ ] **Step 5: Commit caller migration**

```bash
git add egon-cola-components
git commit -m "refactor: migrate common record callers"
```

---

### Task 6: Full Common Verification and Documentation

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/README.md`
- Modify tests if needed based on final verification only.

- [ ] **Step 1: Update README migration notes**

Update the README to state:

```markdown
## Record Contracts

`common-result` and `common-model` expose record contracts. Callers should use record accessors such as `data()`, `records()`, `pageNo()`, and `success()`. JavaBean getters and setters are intentionally not provided.
```

- [ ] **Step 2: Run full common reactor tests**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test
```

Expected: PASS.

- [ ] **Step 3: Run affected components test slice**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-config-center -am test
```

Expected: PASS, unless an unrelated pre-existing environment-bound test fails. If it fails, record exact failing test and reason before deciding whether to patch.

- [ ] **Step 4: Check diff hygiene**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; only intended changed files are present.

- [ ] **Step 5: Commit verification/docs**

```bash
git add egon-cola-components/egon-cola-component-common/README.md egon-cola-components/egon-cola-component-common
git commit -m "test: lock common enterprise contracts"
```

---

## Final Completion Checklist

- [ ] `common-core` has no Jackson, Lombok, Spring, or other common-module imports.
- [ ] `CommonStatus.BUSINESS_ERROR` is `600000`.
- [ ] Unknown exceptions passed to `ResultDtos.failure(Throwable)` return the safe system error message.
- [ ] Result DTO/model contracts are records.
- [ ] Model/query/request contracts are records.
- [ ] No compatibility getter/setter bridge methods were added.
- [ ] Common reactor test command passes.
- [ ] Affected component test command is run or its blocker is reported.

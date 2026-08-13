package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.repository.IdempotencyRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.IdempotencyCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.StoredCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.IdempotencyClaimVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyOutcomeEnum;

/**
 * 类型 `IdempotencyService` 位于当前包内，是类型，用于承载 `Idempotency Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `IdempotencyService` is a type in its package and carries the responsibility, state, or contract for `Idempotency Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Claims idempotency keys without persisting request bodies or sensitive responses.
 */
public final class IdempotencyService {

    /**
     * 字段 `store` 表示 `IdempotencyService` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `IdempotencyRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `IdempotencyService` (declared type `IdempotencyRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `IdempotencyService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `IdempotencyService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdempotencyRepository store;

    /**
     * 构造器 `IdempotencyService` 用于创建并初始化 `IdempotencyService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `IdempotencyService` creates and initializes `IdempotencyService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `IdempotencyService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `IdempotencyService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public IdempotencyService(IdempotencyRepository store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * 方法 `claim` 按照 `IdempotencyService` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `IdempotencyService`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public IdempotencyClaimVO claim(IdempotencyCommandDTO command) {
        String keyHash = sha256(command.idempotencyKey());
        String requestHash = sha256(command.canonicalRequest());
        IdempotencyClaimVO claim = store.claim(new StoredCommandDTO(
                command.tenantId(), command.actorType(), command.actorId(),
                command.operationCode(), keyHash, requestHash,
                command.expiresAt(), command.now()));
        if (claim.outcome() == IdempotencyOutcomeEnum.CONFLICT) {
            throw new Rbac3RuleViolation("IDEMPOTENCY_CONFLICT");
        }
        if (claim.outcome() == IdempotencyOutcomeEnum.IN_PROGRESS) {
            throw new Rbac3RuleViolation("AUTH_MUTATION_CONFLICT");
        }
        return claim;
    }

    /**
     * 方法 `complete` 按照 `IdempotencyService` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `IdempotencyService`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
     *
     * @param recordId 输入参数 `recordId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceType 输入参数 `resourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param responseStatus 输入参数 `responseStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param safeResponseDigest 输入参数 `safeResponseDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void complete(
            String recordId,
            String resourceType,
            String resourceId,
            int responseStatus,
            String safeResponseDigest,
            Instant now
    ) {
        store.complete(
                recordId, resourceType, resourceId, responseStatus,
                sha256(safeResponseDigest), now);
    }






    /**
     * 方法 `sha256` 按照 `IdempotencyService` 的职责处理输入，完成 `sha256` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sha256` processes its inputs according to `IdempotencyService`'s responsibility, performs the `sha256` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sha256` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sha256`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String sha256(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("idempotency value is required");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

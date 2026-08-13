package top.egon.cola.platform.rbac3.admin.runtime.repository;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.StoredCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.IdempotencyClaimVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.IdempotencyService;

/**
     * 类型 `IdempotencyRepository` 位于 `IdempotencyService` 内，是接口，用于承载 `Idempotency Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IdempotencyRepository` is an interface inside `IdempotencyService` and carries the responsibility, state, or contract for `Idempotency Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IdempotencyRepository` 作为 `IdempotencyService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IdempotencyRepository` as the responsibility boundary of `IdempotencyService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface IdempotencyRepository {
        /**
         * 方法 `claim` 按照 `IdempotencyRepository` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `claim` processes its inputs according to `IdempotencyRepository`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        IdempotencyClaimVO claim(StoredCommandDTO command);

        /**
         * 方法 `complete` 按照 `IdempotencyRepository` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `complete` processes its inputs according to `IdempotencyRepository`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
         *
         * @param recordId 输入参数 `recordId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceType 输入参数 `resourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param responseStatus 输入参数 `responseStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param responseDigest 输入参数 `responseDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void complete(
                String recordId,
                String resourceType,
                String resourceId,
                int responseStatus,
                String responseDigest,
                Instant now);
    }

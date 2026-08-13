package top.egon.cola.platform.rbac3.admin.runtime.application;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 类型 `IdempotencyService` 位于当前包内，是类型，用于承载 `Idempotency Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `IdempotencyService` is a type in its package and carries the responsibility, state, or contract for `Idempotency Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Claims idempotency keys without persisting request bodies or sensitive responses.
 */
public final class IdempotencyService {

    /**
     * 字段 `store` 表示 `IdempotencyService` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `IdempotencyStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `IdempotencyService` (declared type `IdempotencyStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `IdempotencyService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `IdempotencyService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdempotencyStore store;

    /**
     * 构造器 `IdempotencyService` 用于创建并初始化 `IdempotencyService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `IdempotencyService` creates and initializes `IdempotencyService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `IdempotencyService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `IdempotencyService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public IdempotencyService(IdempotencyStore store) {
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
    public Claim claim(Command command) {
        String keyHash = sha256(command.idempotencyKey());
        String requestHash = sha256(command.canonicalRequest());
        Claim claim = store.claim(new StoredCommand(
                command.tenantId(), command.actorType(), command.actorId(),
                command.operationCode(), keyHash, requestHash,
                command.expiresAt(), command.now()));
        if (claim.outcome() == Outcome.CONFLICT) {
            throw new Rbac3RuleViolation("IDEMPOTENCY_CONFLICT");
        }
        if (claim.outcome() == Outcome.IN_PROGRESS) {
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
     * 类型 `IdempotencyStore` 位于 `IdempotencyService` 内，是接口，用于承载 `Idempotency Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IdempotencyStore` is an interface inside `IdempotencyService` and carries the responsibility, state, or contract for `Idempotency Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IdempotencyStore` 作为 `IdempotencyService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IdempotencyStore` as the responsibility boundary of `IdempotencyService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface IdempotencyStore {
        /**
         * 方法 `claim` 按照 `IdempotencyStore` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `claim` processes its inputs according to `IdempotencyStore`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Claim claim(StoredCommand command);

        /**
         * 方法 `complete` 按照 `IdempotencyStore` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `complete` processes its inputs according to `IdempotencyStore`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    /**
     * 类型 `Command` 位于 `IdempotencyService` 内，是记录类型，用于承载 `Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Command` is a record inside `IdempotencyService` and carries the responsibility, state, or contract for `Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Command` 作为 `IdempotencyService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Command` as the responsibility boundary of `IdempotencyService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param actorType 记录组件 `actorType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorType` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param operationCode 记录组件 `operationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operationCode` carries constructor data whose meaning is defined by the record contract.
     * @param idempotencyKey 记录组件 `idempotencyKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `idempotencyKey` carries constructor data whose meaning is defined by the record contract.
     * @param canonicalRequest 记录组件 `canonicalRequest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `canonicalRequest` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param now 记录组件 `now` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `now` carries constructor data whose meaning is defined by the record contract.
     */
    public record Command(
            /**
             * 字段 `tenantId` 表示 `Command` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Command` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `actorType` 表示 `Command` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `Command` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorType` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorType`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorType,
            /**
             * 字段 `actorId` 表示 `Command` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `Command` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `operationCode` 表示 `Command` 中与 `operation Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operationCode` stores the `operation Code`-related state, dependency, configuration, or result of `Command` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operationCode` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operationCode`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            String operationCode,
            /**
             * 字段 `idempotencyKey` 表示 `Command` 中与 `idempotency Key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `idempotencyKey` stores the `idempotency Key`-related state, dependency, configuration, or result of `Command` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `idempotencyKey` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `idempotencyKey`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            String idempotencyKey,
            /**
             * 字段 `canonicalRequest` 表示 `Command` 中与 `canonical Request` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `canonicalRequest` stores the `canonical Request`-related state, dependency, configuration, or result of `Command` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `canonicalRequest` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `canonicalRequest`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            String canonicalRequest,
            /**
             * 字段 `expiresAt` 表示 `Command` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `Command` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `now` 表示 `Command` 中与 `now` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `now` stores the `now`-related state, dependency, configuration, or result of `Command` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `now` 时应保持 `Command` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `now`, preserve `Command`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant now
    ) {
    }

    /**
     * 类型 `StoredCommand` 位于 `IdempotencyService` 内，是记录类型，用于承载 `Stored Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `StoredCommand` is a record inside `IdempotencyService` and carries the responsibility, state, or contract for `Stored Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `StoredCommand` 作为 `IdempotencyService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `StoredCommand` as the responsibility boundary of `IdempotencyService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param actorType 记录组件 `actorType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorType` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param operationCode 记录组件 `operationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operationCode` carries constructor data whose meaning is defined by the record contract.
     * @param keyHash 记录组件 `keyHash` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `keyHash` carries constructor data whose meaning is defined by the record contract.
     * @param requestHash 记录组件 `requestHash` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestHash` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param now 记录组件 `now` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `now` carries constructor data whose meaning is defined by the record contract.
     */
    public record StoredCommand(
            /**
             * 字段 `tenantId` 表示 `StoredCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `StoredCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `actorType` 表示 `StoredCommand` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `StoredCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorType` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorType`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorType,
            /**
             * 字段 `actorId` 表示 `StoredCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `StoredCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `operationCode` 表示 `StoredCommand` 中与 `operation Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operationCode` stores the `operation Code`-related state, dependency, configuration, or result of `StoredCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operationCode` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operationCode`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String operationCode,
            /**
             * 字段 `keyHash` 表示 `StoredCommand` 中与 `key Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `keyHash` stores the `key Hash`-related state, dependency, configuration, or result of `StoredCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `keyHash` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `keyHash`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String keyHash,
            /**
             * 字段 `requestHash` 表示 `StoredCommand` 中与 `request Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestHash` stores the `request Hash`-related state, dependency, configuration, or result of `StoredCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestHash` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestHash`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestHash,
            /**
             * 字段 `expiresAt` 表示 `StoredCommand` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `StoredCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `now` 表示 `StoredCommand` 中与 `now` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `now` stores the `now`-related state, dependency, configuration, or result of `StoredCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `now` 时应保持 `StoredCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `now`, preserve `StoredCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant now
    ) {
    }

    /**
     * 类型 `Claim` 位于 `IdempotencyService` 内，是记录类型，用于承载 `Claim` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Claim` is a record inside `IdempotencyService` and carries the responsibility, state, or contract for `Claim`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Claim` 作为 `IdempotencyService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Claim` as the responsibility boundary of `IdempotencyService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param recordId 记录组件 `recordId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `recordId` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param resourceId 记录组件 `resourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceId` carries constructor data whose meaning is defined by the record contract.
     * @param responseStatus 记录组件 `responseStatus` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `responseStatus` carries constructor data whose meaning is defined by the record contract.
     * @param responseDigest 记录组件 `responseDigest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `responseDigest` carries constructor data whose meaning is defined by the record contract.
     */
    public record Claim(
            /**
             * 字段 `recordId` 表示 `Claim` 中与 `record Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `recordId` stores the `record Id`-related state, dependency, configuration, or result of `Claim` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `recordId` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `recordId`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
             */
            String recordId,
            /**
             * 字段 `outcome` 表示 `Claim` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `Claim` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
             */
            Outcome outcome,
            /**
             * 字段 `resourceId` 表示 `Claim` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `Claim` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceId,
            /**
             * 字段 `responseStatus` 表示 `Claim` 中与 `response Status` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `responseStatus` stores the `response Status`-related state, dependency, configuration, or result of `Claim` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `responseStatus` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `responseStatus`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer responseStatus,
            /**
             * 字段 `responseDigest` 表示 `Claim` 中与 `response Digest` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `responseDigest` stores the `response Digest`-related state, dependency, configuration, or result of `Claim` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `responseDigest` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `responseDigest`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
             */
            String responseDigest
    ) {
    }

    /**
     * 类型 `Outcome` 位于 `IdempotencyService` 内，是枚举，用于承载 `Outcome` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Outcome` is an enum inside `IdempotencyService` and carries the responsibility, state, or contract for `Outcome`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Outcome` 作为 `IdempotencyService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Outcome` as the responsibility boundary of `IdempotencyService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Outcome {
        /**
         * 字段 `CLAIMED` 表示 `Outcome` 中与 `CLAIMED` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `CLAIMED` stores the `CLAIMED`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `CLAIMED` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `CLAIMED`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        CLAIMED,
        /**
         * 字段 `REPLAY` 表示 `Outcome` 中与 `REPLAY` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REPLAY` stores the `REPLAY`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REPLAY` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REPLAY`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        REPLAY,
        /**
         * 字段 `IN_PROGRESS` 表示 `Outcome` 中与 `IN PROGRESS` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `IN_PROGRESS` stores the `IN PROGRESS`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `IN_PROGRESS` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `IN_PROGRESS`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        IN_PROGRESS,
        /**
         * 字段 `CONFLICT` 表示 `Outcome` 中与 `CONFLICT` 相关的状态、依赖、配置或结果（声明类型 `Outcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `CONFLICT` stores the `CONFLICT`-related state, dependency, configuration, or result of `Outcome` (declared type `Outcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `CONFLICT` 时应保持 `Outcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `CONFLICT`, preserve `Outcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        CONFLICT
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

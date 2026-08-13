package top.egon.cola.platform.rbac3.admin.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 类型 `IdentityMappingFacade` 位于当前包内，是类型，用于承载 `Identity Mapping Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `IdentityMappingFacade` is a type in its package and carries the responsibility, state, or contract for `Identity Mapping Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Owns the one-to-one mapping between a global IdP identity and a tenant user.
 */
public final class IdentityMappingFacade {

    /**
     * 字段 `store` 表示 `IdentityMappingFacade` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `MappingStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `IdentityMappingFacade` (declared type `MappingStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `IdentityMappingFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `IdentityMappingFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final MappingStore store;
    /**
     * 字段 `idGenerator` 表示 `IdentityMappingFacade` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `MappingIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `IdentityMappingFacade` (declared type `MappingIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `IdentityMappingFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `IdentityMappingFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final MappingIdGenerator idGenerator;

    /**
     * 构造器 `IdentityMappingFacade` 用于创建并初始化 `IdentityMappingFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `IdentityMappingFacade` creates and initializes `IdentityMappingFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `IdentityMappingFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `IdentityMappingFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public IdentityMappingFacade(MappingStore store, MappingIdGenerator idGenerator) {
        this.store = Objects.requireNonNull(store, "store");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    /**
     * 方法 `bind` 按照 `IdentityMappingFacade` 的职责处理输入，完成 `bind` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bind` processes its inputs according to `IdentityMappingFacade`'s responsibility, performs the `bind` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bind` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bind`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rbac3UserId 输入参数 `rbac3UserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Mapping bind(
            String tenantId,
            String identitySub,
            String rbac3UserId,
            String actorId,
            Instant now) {
        String normalizedTenantId = required(tenantId, "tenantId");
        String normalizedSub = required(identitySub, "identitySub");
        String normalizedUserId = required(rbac3UserId, "rbac3UserId");
        required(actorId, "actorId");
        Objects.requireNonNull(now, "now");
        Optional<Mapping> existing = store.find(normalizedTenantId, normalizedSub);
        if (existing.isPresent()) {
            Mapping mapping = existing.orElseThrow();
            if (!mapping.rbac3UserId().equals(normalizedUserId)) {
                throw new DuplicateIdentityMappingException(
                        normalizedTenantId, normalizedSub, mapping.rbac3UserId());
            }
            return mapping;
        }
        return store.create(
                idGenerator.nextId(), normalizedTenantId, normalizedSub,
                normalizedUserId, actorId.trim(), now);
    }

    /**
     * 方法 `resolve` 按照 `IdentityMappingFacade` 的职责处理输入，完成 `resolve` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resolve` processes its inputs according to `IdentityMappingFacade`'s responsibility, performs the `resolve` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resolve` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resolve`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clientId 输入参数 `clientId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Optional<ResolvedMembership> resolve(
            String identitySub,
            String tenantId,
            String clientId) {
        required(clientId, "clientId");
        return store.resolve(
                required(tenantId, "tenantId"), required(identitySub, "identitySub"));
    }

    /**
     * 方法 `tenants` 按照 `IdentityMappingFacade` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenants` processes its inputs according to `IdentityMappingFacade`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clientId 输入参数 `clientId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<TenantMembership> tenants(String identitySub, String clientId) {
        required(clientId, "clientId");
        return List.copyOf(store.tenants(required(identitySub, "identitySub")));
    }

    /**
     * 方法 `required` 按照 `IdentityMappingFacade` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `IdentityMappingFacade`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `MappingStore` 位于 `IdentityMappingFacade` 内，是接口，用于承载 `Mapping Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MappingStore` is an interface inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Mapping Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MappingStore` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MappingStore` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface MappingStore {

        /**
         * 方法 `find` 按照 `MappingStore` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `find` processes its inputs according to `MappingStore`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<Mapping> find(String tenantId, String identitySub);

        /**
         * 方法 `create` 按照 `MappingStore` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `MappingStore`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mappingId 输入参数 `mappingId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rbac3UserId 输入参数 `rbac3UserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Mapping create(
                long mappingId,
                String tenantId,
                String identitySub,
                String rbac3UserId,
                String actorId,
                Instant now);

        /**
         * 方法 `resolve` 按照 `MappingStore` 的职责处理输入，完成 `resolve` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `resolve` processes its inputs according to `MappingStore`'s responsibility, performs the `resolve` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `resolve` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `resolve`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<ResolvedMembership> resolve(String tenantId, String identitySub);

        /**
         * 方法 `tenants` 按照 `MappingStore` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `tenants` processes its inputs according to `MappingStore`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<TenantMembership> tenants(String identitySub);
    }

    /**
     * 类型 `MappingIdGenerator` 位于 `IdentityMappingFacade` 内，是接口，用于承载 `Mapping Id Generator` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MappingIdGenerator` is an interface inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Mapping Id Generator`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MappingIdGenerator` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MappingIdGenerator` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface MappingIdGenerator {

        /**
         * 方法 `nextId` 按照 `MappingIdGenerator` 的职责处理输入，完成 `next Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `nextId` processes its inputs according to `MappingIdGenerator`'s responsibility, performs the `next Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `nextId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `nextId`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        long nextId();
    }

    /**
     * 类型 `Mapping` 位于 `IdentityMappingFacade` 内，是记录类型，用于承载 `Mapping` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Mapping` is a record inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Mapping`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Mapping` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Mapping` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param mappingId 记录组件 `mappingId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mappingId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param active 记录组件 `active` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `active` carries constructor data whose meaning is defined by the record contract.
     * @param updatedAt 记录组件 `updatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `updatedAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record Mapping(
            /**
             * 字段 `mappingId` 表示 `Mapping` 中与 `mapping Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mappingId` stores the `mapping Id`-related state, dependency, configuration, or result of `Mapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mappingId` 时应保持 `Mapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mappingId`, preserve `Mapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mappingId,
            /**
             * 字段 `tenantId` 表示 `Mapping` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Mapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Mapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Mapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `identitySub` 表示 `Mapping` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `Mapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `Mapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `Mapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `rbac3UserId` 表示 `Mapping` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `Mapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `Mapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `Mapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `active` 表示 `Mapping` 中与 `active` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `active` stores the `active`-related state, dependency, configuration, or result of `Mapping` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `active` 时应保持 `Mapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `active`, preserve `Mapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean active,
            /**
             * 字段 `updatedAt` 表示 `Mapping` 中与 `updated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `updatedAt` stores the `updated At`-related state, dependency, configuration, or result of `Mapping` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `updatedAt` 时应保持 `Mapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `updatedAt`, preserve `Mapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant updatedAt
    ) {
    }

    /**
     * 类型 `ResolvedMembership` 位于 `IdentityMappingFacade` 内，是记录类型，用于承载 `Resolved Membership` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResolvedMembership` is a record inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Resolved Membership`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResolvedMembership` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResolvedMembership` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantCode 记录组件 `tenantCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantCode` carries constructor data whose meaning is defined by the record contract.
     * @param tenantName 记录组件 `tenantName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantName` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param displayName 记录组件 `displayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `displayName` carries constructor data whose meaning is defined by the record contract.
     * @param authorizationContextRequired 记录组件 `authorizationContextRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authorizationContextRequired` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResolvedMembership(
            /**
             * 字段 `tenantId` 表示 `ResolvedMembership` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `tenantCode` 表示 `ResolvedMembership` 中与 `tenant Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantCode` stores the `tenant Code`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantCode` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantCode`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantCode,
            /**
             * 字段 `tenantName` 表示 `ResolvedMembership` 中与 `tenant Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantName` stores the `tenant Name`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantName` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantName`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantName,
            /**
             * 字段 `identitySub` 表示 `ResolvedMembership` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `rbac3UserId` 表示 `ResolvedMembership` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `displayName` 表示 `ResolvedMembership` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayName` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayName`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String displayName,
            /**
             * 字段 `authorizationContextRequired` 表示 `ResolvedMembership` 中与 `authorization Context Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authorizationContextRequired` stores the `authorization Context Required`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authorizationContextRequired` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authorizationContextRequired`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean authorizationContextRequired,
            /**
             * 字段 `authVersion` 表示 `ResolvedMembership` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `ResolvedMembership` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ResolvedMembership` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResolvedMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ResolvedMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion
    ) {
    }

    /**
     * 类型 `TenantMembership` 位于 `IdentityMappingFacade` 内，是记录类型，用于承载 `Tenant Membership` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TenantMembership` is a record inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Tenant Membership`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TenantMembership` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TenantMembership` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantCode 记录组件 `tenantCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantCode` carries constructor data whose meaning is defined by the record contract.
     * @param tenantName 记录组件 `tenantName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantName` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param displayName 记录组件 `displayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `displayName` carries constructor data whose meaning is defined by the record contract.
     */
    public record TenantMembership(
            /**
             * 字段 `tenantId` 表示 `TenantMembership` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TenantMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TenantMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TenantMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `tenantCode` 表示 `TenantMembership` 中与 `tenant Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantCode` stores the `tenant Code`-related state, dependency, configuration, or result of `TenantMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantCode` 时应保持 `TenantMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantCode`, preserve `TenantMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantCode,
            /**
             * 字段 `tenantName` 表示 `TenantMembership` 中与 `tenant Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantName` stores the `tenant Name`-related state, dependency, configuration, or result of `TenantMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantName` 时应保持 `TenantMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantName`, preserve `TenantMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantName,
            /**
             * 字段 `rbac3UserId` 表示 `TenantMembership` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `TenantMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `TenantMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `TenantMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `displayName` 表示 `TenantMembership` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `TenantMembership` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayName` 时应保持 `TenantMembership` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayName`, preserve `TenantMembership`'s lifecycle, immutability, and thread-safety constraints.
             */
            String displayName
    ) {
    }

    /**
     * 类型 `DuplicateIdentityMappingException` 位于 `IdentityMappingFacade` 内，是类型，用于承载 `Duplicate Identity Mapping Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DuplicateIdentityMappingException` is a type inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Duplicate Identity Mapping Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DuplicateIdentityMappingException` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DuplicateIdentityMappingException` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public static final class DuplicateIdentityMappingException
            extends IllegalStateException {

        /**
         * 构造器 `DuplicateIdentityMappingException` 用于创建并初始化 `DuplicateIdentityMappingException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DuplicateIdentityMappingException` creates and initializes `DuplicateIdentityMappingException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DuplicateIdentityMappingException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DuplicateIdentityMappingException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param existingUserId 输入参数 `existingUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DuplicateIdentityMappingException(
                String tenantId, String identitySub, String existingUserId) {
            super("identity already maps to another tenant user: tenantId="
                    + tenantId + ", identitySub=" + identitySub
                    + ", rbac3UserId=" + existingUserId);
        }
    }
}

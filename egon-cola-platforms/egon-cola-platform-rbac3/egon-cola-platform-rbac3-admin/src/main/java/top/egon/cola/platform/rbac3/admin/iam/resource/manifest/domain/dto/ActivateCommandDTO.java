package top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.dto;

import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
     * 类型 `ActivateCommandDTO` 位于 `ManifestFacade` 内，是记录类型，用于承载 `Activate Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivateCommandDTO` is a record inside `ManifestFacade` and carries the responsibility, state, or contract for `Activate Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivateCommandDTO` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivateCommandDTO` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param manifestId 记录组件 `manifestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedApplicationVersion 记录组件 `expectedApplicationVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedApplicationVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedCurrentManifestVersion 记录组件 `expectedCurrentManifestVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedCurrentManifestVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedDefinitionSetId 记录组件 `expectedDefinitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedDefinitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param idempotencyKey 记录组件 `idempotencyKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `idempotencyKey` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivateCommandDTO(
            /**
             * 字段 `tenantId` 表示 `ActivateCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `ActivateCommandDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `manifestId` 表示 `ActivateCommandDTO` 中与 `manifest Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifestId` stores the `manifest Id`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifestId` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifestId`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String manifestId,
            /**
             * 字段 `expectedApplicationVersion` 表示 `ActivateCommandDTO` 中与 `expected Application Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedApplicationVersion` stores the `expected Application Version`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedApplicationVersion` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedApplicationVersion`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedApplicationVersion,
            /**
             * 字段 `expectedCurrentManifestVersion` 表示 `ActivateCommandDTO` 中与 `expected Current Manifest Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedCurrentManifestVersion` stores the `expected Current Manifest Version`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedCurrentManifestVersion` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedCurrentManifestVersion`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedCurrentManifestVersion,
            /**
             * 字段 `expectedDefinitionSetId` 表示 `ActivateCommandDTO` 中与 `expected Definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedDefinitionSetId` stores the `expected Definition Set Id`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedDefinitionSetId` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedDefinitionSetId`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String expectedDefinitionSetId,
            /**
             * 字段 `actorId` 表示 `ActivateCommandDTO` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `idempotencyKey` 表示 `ActivateCommandDTO` 中与 `idempotency Key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `idempotencyKey` stores the `idempotency Key`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `idempotencyKey` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `idempotencyKey`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String idempotencyKey,
            /**
             * 字段 `reason` 表示 `ActivateCommandDTO` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `ActivateCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `ActivateCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `ActivateCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason
    ) {

        /**
         * 构造器 `ActivateCommandDTO` 用于创建并初始化 `ActivateCommandDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ActivateCommandDTO` creates and initializes `ActivateCommandDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ActivateCommandDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ActivateCommandDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedApplicationVersion 输入参数 `expectedApplicationVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedCurrentManifestVersion 输入参数 `expectedCurrentManifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedDefinitionSetId 输入参数 `expectedDefinitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
    public ActivateCommandDTO {
            tenantId = required(tenantId, "tenantId");
            applicationId = required(applicationId, "applicationId");
            manifestId = required(manifestId, "manifestId");
            expectedDefinitionSetId = required(
                    expectedDefinitionSetId, "expectedDefinitionSetId");
            actorId = required(actorId, "actorId");
            idempotencyKey = required(idempotencyKey, "idempotencyKey");
            reason = required(reason, "reason");
        if (expectedApplicationVersion < 0L || expectedCurrentManifestVersion < 0L) {
            throw new IllegalArgumentException("manifest versions must not be negative");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

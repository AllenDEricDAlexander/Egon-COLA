package top.egon.cola.platform.rbac3.admin.directory.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.directory.domain.UserPositionKey;

/**
     * 类型 `AssignmentSignature` 位于 `DirectorySnapshotMaterializer` 内，是记录类型，用于承载 `Assignment Signature` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentSignature` is a record inside `DirectorySnapshotMaterializer` and carries the responsibility, state, or contract for `Assignment Signature`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentSignature` 作为 `DirectorySnapshotMaterializer` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentSignature` as the responsibility boundary of `DirectorySnapshotMaterializer`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param positionId 记录组件 `positionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `positionId` carries constructor data whose meaning is defined by the record contract.
     * @param orgUnitId 记录组件 `orgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `orgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param primary 记录组件 `primary` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `primary` carries constructor data whose meaning is defined by the record contract.
     * @param externalAssignmentId 记录组件 `externalAssignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `externalAssignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     */
    record AssignmentSignature(
            /**
             * 字段 `userId` 表示 `AssignmentSignature` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long userId,
            /**
             * 字段 `positionId` 表示 `AssignmentSignature` 中与 `position Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `positionId` stores the `position Id`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `positionId` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `positionId`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long positionId,
            /**
             * 字段 `orgUnitId` 表示 `AssignmentSignature` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long orgUnitId,
            /**
             * 字段 `primary` 表示 `AssignmentSignature` 中与 `primary` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `primary` stores the `primary`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `primary` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `primary`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean primary,
            /**
             * 字段 `externalAssignmentId` 表示 `AssignmentSignature` 中与 `external Assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `externalAssignmentId` stores the `external Assignment Id`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `externalAssignmentId` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `externalAssignmentId`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            String externalAssignmentId,
            /**
             * 字段 `validFrom` 表示 `AssignmentSignature` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignmentSignature` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignmentSignature` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignmentSignature` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignmentSignature`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo) {

        /**
         * 方法 `key` 按照 `AssignmentSignature` 的职责处理输入，完成 `key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `key` processes its inputs according to `AssignmentSignature`'s responsibility, performs the `key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `key` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `key`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        UserPositionKey key() {
            return new UserPositionKey(userId, positionId);
        }
    }

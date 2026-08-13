package top.egon.cola.platform.rbac3.admin.identity.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
     * 类型 `UserDirectoryVO` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `User Directory View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UserDirectoryVO` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `User Directory View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UserDirectoryVO` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UserDirectoryVO` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param username 记录组件 `username` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `username` carries constructor data whose meaning is defined by the record contract.
     * @param displayName 记录组件 `displayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `displayName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param primaryOrgUnitId 记录组件 `primaryOrgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `primaryOrgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param primaryPositionId 记录组件 `primaryPositionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `primaryPositionId` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record UserDirectoryVO(
            /**
             * 字段 `userId` 表示 `UserDirectoryVO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `username` 表示 `UserDirectoryVO` 中与 `username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `username` stores the `username`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `username` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `username`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String username,
            /**
             * 字段 `displayName` 表示 `UserDirectoryVO` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayName` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayName`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String displayName,
            /**
             * 字段 `status` 表示 `UserDirectoryVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authVersion` 表示 `UserDirectoryVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `primaryOrgUnitId` 表示 `UserDirectoryVO` 中与 `primary Org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `primaryOrgUnitId` stores the `primary Org Unit Id`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `primaryOrgUnitId` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `primaryOrgUnitId`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String primaryOrgUnitId,
            /**
             * 字段 `primaryPositionId` 表示 `UserDirectoryVO` 中与 `primary Position Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `primaryPositionId` stores the `primary Position Id`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `primaryPositionId` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `primaryPositionId`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String primaryPositionId,
            /**
             * 字段 `directorySnapshotVersion` 表示 `UserDirectoryVO` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `UserDirectoryVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `UserDirectoryVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `UserDirectoryVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long directorySnapshotVersion
    ) {
    }

package top.egon.cola.platform.rbac3.admin.directory.domain.dto;

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
     * 类型 `DirectorySnapshotCommandDTO` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Directory Snapshot Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectorySnapshotCommandDTO` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Snapshot Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectorySnapshotCommandDTO` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectorySnapshotCommandDTO` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param providerCode 记录组件 `providerCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `providerCode` carries constructor data whose meaning is defined by the record contract.
     * @param snapshotVersion 记录组件 `snapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param checksum 记录组件 `checksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checksum` carries constructor data whose meaning is defined by the record contract.
     * @param generatedAt 记录组件 `generatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `generatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param payload 记录组件 `payload` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payload` carries constructor data whose meaning is defined by the record contract.
     */
    public record DirectorySnapshotCommandDTO(
            /**
             * 字段 `providerCode` 表示 `DirectorySnapshotCommandDTO` 中与 `provider Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `providerCode` stores the `provider Code`-related state, dependency, configuration, or result of `DirectorySnapshotCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `providerCode` 时应保持 `DirectorySnapshotCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `providerCode`, preserve `DirectorySnapshotCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String providerCode,
            /**
             * 字段 `snapshotVersion` 表示 `DirectorySnapshotCommandDTO` 中与 `snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotVersion` stores the `snapshot Version`-related state, dependency, configuration, or result of `DirectorySnapshotCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotVersion` 时应保持 `DirectorySnapshotCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotVersion`, preserve `DirectorySnapshotCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long snapshotVersion,
            /**
             * 字段 `checksum` 表示 `DirectorySnapshotCommandDTO` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `DirectorySnapshotCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checksum` 时应保持 `DirectorySnapshotCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checksum`, preserve `DirectorySnapshotCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String checksum,
            /**
             * 字段 `generatedAt` 表示 `DirectorySnapshotCommandDTO` 中与 `generated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `generatedAt` stores the `generated At`-related state, dependency, configuration, or result of `DirectorySnapshotCommandDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `generatedAt` 时应保持 `DirectorySnapshotCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `generatedAt`, preserve `DirectorySnapshotCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant generatedAt,
            /**
             * 字段 `payload` 表示 `DirectorySnapshotCommandDTO` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `DirectorySnapshotCommandDTO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payload` 时应保持 `DirectorySnapshotCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payload`, preserve `DirectorySnapshotCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            @NotNull Map<String, Object> payload
    ) {
    }

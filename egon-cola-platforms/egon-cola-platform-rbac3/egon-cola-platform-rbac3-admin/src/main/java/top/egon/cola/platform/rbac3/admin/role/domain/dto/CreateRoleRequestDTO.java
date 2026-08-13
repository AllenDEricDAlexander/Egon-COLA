package top.egon.cola.platform.rbac3.admin.role.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.List;

/**
     * 类型 `CreateRoleRequestDTO` 位于 `RolePermissionController` 内，是记录类型，用于承载 `Create Role Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CreateRoleRequestDTO` is a record inside `RolePermissionController` and carries the responsibility, state, or contract for `Create Role Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CreateRoleRequestDTO` 作为 `RolePermissionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CreateRoleRequestDTO` as the responsibility boundary of `RolePermissionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleCode 记录组件 `roleCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleCode` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param roleType 记录组件 `roleType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleType` carries constructor data whose meaning is defined by the record contract.
     * @param riskLevel 记录组件 `riskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `riskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     * @param landingRouteId 记录组件 `landingRouteId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingRouteId` carries constructor data whose meaning is defined by the record contract.
     * @param landingPriority 记录组件 `landingPriority` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingPriority` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     */
    public record CreateRoleRequestDTO(
            /**
             * 字段 `applicationId` 表示 `CreateRoleRequestDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `roleCode` 表示 `CreateRoleRequestDTO` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleCode,
            /**
             * 字段 `roleName` 表示 `CreateRoleRequestDTO` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleName,
            /**
             * 字段 `roleType` 表示 `CreateRoleRequestDTO` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleType` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleType`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleType,
            /**
             * 字段 `riskLevel` 表示 `CreateRoleRequestDTO` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String riskLevel,
            /**
             * 字段 `privileged` 表示 `CreateRoleRequestDTO` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged,
            /**
             * 字段 `landingRouteId` 表示 `CreateRoleRequestDTO` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String landingRouteId,
            /**
             * 字段 `landingPriority` 表示 `CreateRoleRequestDTO` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero int landingPriority,
            /**
             * 字段 `maximumAssignmentDays` 表示 `CreateRoleRequestDTO` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `CreateRoleRequestDTO` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `CreateRoleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `CreateRoleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays) {
    }

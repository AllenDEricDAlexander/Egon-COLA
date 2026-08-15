package top.egon.cola.platform.rbac3.admin.iam.role.domain.dto;

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
     * 类型 `BindPermissionsRequestDTO` 位于 `RolePermissionController` 内，是记录类型，用于承载 `Bind Permissions Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BindPermissionsRequestDTO` is a record inside `RolePermissionController` and carries the responsibility, state, or contract for `Bind Permissions Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BindPermissionsRequestDTO` 作为 `RolePermissionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BindPermissionsRequestDTO` as the responsibility boundary of `RolePermissionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionIds 记录组件 `permissionIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionIds` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record BindPermissionsRequestDTO(
            /**
             * 字段 `applicationId` 表示 `BindPermissionsRequestDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `BindPermissionsRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `BindPermissionsRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `BindPermissionsRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `permissionIds` 表示 `BindPermissionsRequestDTO` 中与 `permission Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionIds` stores the `permission Ids`-related state, dependency, configuration, or result of `BindPermissionsRequestDTO` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionIds` 时应保持 `BindPermissionsRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionIds`, preserve `BindPermissionsRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> permissionIds,
            /**
             * 字段 `validFrom` 表示 `BindPermissionsRequestDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `BindPermissionsRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `BindPermissionsRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `BindPermissionsRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `BindPermissionsRequestDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `BindPermissionsRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `BindPermissionsRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `BindPermissionsRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedRoleVersion` 表示 `BindPermissionsRequestDTO` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `BindPermissionsRequestDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `BindPermissionsRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `BindPermissionsRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedRoleVersion) {
    }

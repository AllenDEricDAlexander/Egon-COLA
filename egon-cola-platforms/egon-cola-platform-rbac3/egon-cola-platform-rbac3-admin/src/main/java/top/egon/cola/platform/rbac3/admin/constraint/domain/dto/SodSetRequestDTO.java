package top.egon.cola.platform.rbac3.admin.constraint.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.constraint.service.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;
import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.constraint.controller.ConstraintController;

/**
     * 类型 `SodSetRequestDTO` 位于 `ConstraintController` 内，是记录类型，用于承载 `Sod Set Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SodSetRequestDTO` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Sod Set Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SodSetRequestDTO` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SodSetRequestDTO` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param setCode 记录组件 `setCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `setCode` carries constructor data whose meaning is defined by the record contract.
     * @param constraintType 记录组件 `constraintType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `constraintType` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoles 记录组件 `maximumActiveRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoles` carries constructor data whose meaning is defined by the record contract.
     * @param memberRoleIds 记录组件 `memberRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `memberRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record SodSetRequestDTO(
            /**
             * 字段 `setCode` 表示 `SodSetRequestDTO` 中与 `set Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `setCode` stores the `set Code`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `setCode` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `setCode`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String setCode,
            /**
             * 字段 `constraintType` 表示 `SodSetRequestDTO` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String constraintType,
            /**
             * 字段 `applicationId` 表示 `SodSetRequestDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `maximumActiveRoles` 表示 `SodSetRequestDTO` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @Positive int maximumActiveRoles,
            /**
             * 字段 `memberRoleIds` 表示 `SodSetRequestDTO` 中与 `member Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `memberRoleIds` stores the `member Role Ids`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `memberRoleIds` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `memberRoleIds`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> memberRoleIds,
            /**
             * 字段 `validFrom` 表示 `SodSetRequestDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SodSetRequestDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `SodSetRequestDTO` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SodSetRequestDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SodSetRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SodSetRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }

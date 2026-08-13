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
     * 类型 `OperationSodRuleRequestDTO` 位于 `ConstraintController` 内，是记录类型，用于承载 `Operation Sod Rule Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleRequestDTO` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Operation Sod Rule Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleRequestDTO` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleRequestDTO` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param priorActionCode 记录组件 `priorActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `priorActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param forbiddenLaterActionCode 记录组件 `forbiddenLaterActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `forbiddenLaterActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param lookbackFrom 记录组件 `lookbackFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lookbackFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record OperationSodRuleRequestDTO(
            /**
             * 字段 `applicationCode` 表示 `OperationSodRuleRequestDTO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationCode,
            /**
             * 字段 `businessResource` 表示 `OperationSodRuleRequestDTO` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String businessResource,
            /**
             * 字段 `priorActionCode` 表示 `OperationSodRuleRequestDTO` 中与 `prior Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `priorActionCode` stores the `prior Action Code`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `priorActionCode` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `priorActionCode`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String priorActionCode,
            /**
             * 字段 `forbiddenLaterActionCode` 表示 `OperationSodRuleRequestDTO` 中与 `forbidden Later Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `forbiddenLaterActionCode` stores the `forbidden Later Action Code`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `forbiddenLaterActionCode` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `forbiddenLaterActionCode`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String forbiddenLaterActionCode,
            /**
             * 字段 `lookbackFrom` 表示 `OperationSodRuleRequestDTO` 中与 `lookback From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lookbackFrom` stores the `lookback From`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lookbackFrom` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lookbackFrom`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lookbackFrom,
            /**
             * 字段 `validFrom` 表示 `OperationSodRuleRequestDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `OperationSodRuleRequestDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `OperationSodRuleRequestDTO` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `OperationSodRuleRequestDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `OperationSodRuleRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `OperationSodRuleRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }

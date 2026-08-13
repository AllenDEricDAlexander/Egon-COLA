package top.egon.cola.platform.rbac3.admin.management.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
     * 类型 `PolicyRequestDTO` 位于 `ManagementPolicyController` 内，是记录类型，用于承载 `Policy Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PolicyRequestDTO` is a record inside `ManagementPolicyController` and carries the responsibility, state, or contract for `Policy Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PolicyRequestDTO` 作为 `ManagementPolicyController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PolicyRequestDTO` as the responsibility boundary of `ManagementPolicyController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param restrictions 记录组件 `restrictions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `restrictions` carries constructor data whose meaning is defined by the record contract.
     */
    public record PolicyRequestDTO(
            /**
             * 字段 `policyCode` 表示 `PolicyRequestDTO` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String policyCode,
            /**
             * 字段 `name` 表示 `PolicyRequestDTO` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String name,
            /**
             * 字段 `validFrom` 表示 `PolicyRequestDTO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `PolicyRequestDTO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `subjects` 表示 `PolicyRequestDTO` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@Valid ManagementPolicySubjectDTO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `List&lt;@Valid ManagementPolicySubjectDTO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@Valid ManagementPolicySubjectDTO> subjects,
            /**
             * 字段 `scopes` 表示 `PolicyRequestDTO` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@Valid ManagementPolicyScopeDTO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `List&lt;@Valid ManagementPolicyScopeDTO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@Valid ManagementPolicyScopeDTO> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `PolicyRequestDTO` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `PolicyRequestDTO` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `Set&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty Set<@NotBlank String> operations,
            /**
             * 字段 `restrictions` 表示 `PolicyRequestDTO` 中与 `restrictions` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyRestrictionsDTO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `restrictions` stores the `restrictions`-related state, dependency, configuration, or result of `PolicyRequestDTO` (declared type `ManagementPolicyRestrictionsDTO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `restrictions` 时应保持 `PolicyRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `restrictions`, preserve `PolicyRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull @Valid ManagementPolicyRestrictionsDTO restrictions
    ) {
        /**
         * 构造器 `PolicyRequestDTO` 用于创建并初始化 `PolicyRequestDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyRequestDTO` creates and initializes `PolicyRequestDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyRequestDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyRequestDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param restrictions 输入参数 `restrictions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PolicyRequestDTO {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = activationRootRoleIds.stream()
                    .sorted(Comparator.naturalOrder()).toList();
            operations = Set.copyOf(operations);
        }
    }

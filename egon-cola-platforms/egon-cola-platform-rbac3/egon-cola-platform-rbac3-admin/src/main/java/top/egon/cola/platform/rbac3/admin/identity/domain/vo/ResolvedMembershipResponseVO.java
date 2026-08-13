package top.egon.cola.platform.rbac3.admin.identity.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.service.IdentityMappingFacade;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.identity.controller.InternalIdentityController;

/**
     * 类型 `ResolvedMembershipResponseVO` 位于 `InternalIdentityController` 内，是记录类型，用于承载 `Resolved Membership Response` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResolvedMembershipResponseVO` is a record inside `InternalIdentityController` and carries the responsibility, state, or contract for `Resolved Membership Response`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResolvedMembershipResponseVO` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResolvedMembershipResponseVO` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantDisplayName 记录组件 `tenantDisplayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantDisplayName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authorizationContextRequired 记录组件 `authorizationContextRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authorizationContextRequired` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResolvedMembershipResponseVO(
            /**
             * 字段 `identitySub` 表示 `ResolvedMembershipResponseVO` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `tenantId` 表示 `ResolvedMembershipResponseVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `rbac3UserId` 表示 `ResolvedMembershipResponseVO` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `tenantDisplayName` 表示 `ResolvedMembershipResponseVO` 中与 `tenant Display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantDisplayName` stores the `tenant Display Name`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantDisplayName` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantDisplayName`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantDisplayName,
            /**
             * 字段 `status` 表示 `ResolvedMembershipResponseVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authorizationContextRequired` 表示 `ResolvedMembershipResponseVO` 中与 `authorization Context Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authorizationContextRequired` stores the `authorization Context Required`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authorizationContextRequired` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authorizationContextRequired`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean authorizationContextRequired,
            /**
             * 字段 `authVersion` 表示 `ResolvedMembershipResponseVO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `ResolvedMembershipResponseVO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ResolvedMembershipResponseVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResolvedMembershipResponseVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ResolvedMembershipResponseVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion
    ) {
        /**
         * 方法 `from` 按照 `ResolvedMembershipResponseVO` 的职责处理输入，完成 `from` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `from` processes its inputs according to `ResolvedMembershipResponseVO`'s responsibility, performs the `from` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `from` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `from`, then continue the business flow using its result, exception, or side effect.
         *
         * @param membership 输入参数 `membership`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static ResolvedMembershipResponseVO from(
                ResolvedMembershipVO membership) {
            return new ResolvedMembershipResponseVO(
                    membership.identitySub(),
                    membership.tenantId(),
                    membership.rbac3UserId(),
                    membership.tenantName(),
                    "ACTIVE",
                    membership.authorizationContextRequired(),
                    membership.authVersion(),
                    membership.policyVersion());
        }
    }

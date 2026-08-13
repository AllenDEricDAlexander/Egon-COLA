package top.egon.cola.platform.rbac3.admin.session.domain.vo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.session.controller.SessionController;

/**
     * 类型 `SessionVO` 位于 `SessionController` 内，是记录类型，用于承载 `Session View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionVO` is a record inside `SessionController` and carries the responsibility, state, or contract for `Session View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionVO` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionVO` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param authStrength 记录组件 `authStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authStrength` carries constructor data whose meaning is defined by the record contract.
     * @param authenticatedAt 记录组件 `authenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param strongAuthenticatedAt 记录组件 `strongAuthenticatedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `strongAuthenticatedAt` carries constructor data whose meaning is defined by the record contract.
     * @param lastSeenAt 记录组件 `lastSeenAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lastSeenAt` carries constructor data whose meaning is defined by the record contract.
     * @param absoluteExpiresAt 记录组件 `absoluteExpiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `absoluteExpiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record SessionVO(
            /**
             * 字段 `sessionId` 表示 `SessionVO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `SessionVO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `SessionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `sessionVersion` 表示 `SessionVO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `authStrength` 表示 `SessionVO` 中与 `auth Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authStrength` stores the `auth Strength`-related state, dependency, configuration, or result of `SessionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authStrength` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authStrength`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authStrength,
            /**
             * 字段 `authenticatedAt` 表示 `SessionVO` 中与 `authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticatedAt` stores the `authenticated At`-related state, dependency, configuration, or result of `SessionVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticatedAt` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticatedAt`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant authenticatedAt,
            /**
             * 字段 `strongAuthenticatedAt` 表示 `SessionVO` 中与 `strong Authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `strongAuthenticatedAt` stores the `strong Authenticated At`-related state, dependency, configuration, or result of `SessionVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `strongAuthenticatedAt` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `strongAuthenticatedAt`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant strongAuthenticatedAt,
            /**
             * 字段 `lastSeenAt` 表示 `SessionVO` 中与 `last Seen At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lastSeenAt` stores the `last Seen At`-related state, dependency, configuration, or result of `SessionVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lastSeenAt` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lastSeenAt`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lastSeenAt,
            /**
             * 字段 `absoluteExpiresAt` 表示 `SessionVO` 中与 `absolute Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `absoluteExpiresAt` stores the `absolute Expires At`-related state, dependency, configuration, or result of `SessionVO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `absoluteExpiresAt` 时应保持 `SessionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `absoluteExpiresAt`, preserve `SessionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant absoluteExpiresAt
    ) {
    }

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
import java.time.Instant;
import java.util.List;

/**
     * 类型 `RevokeAllVO` 位于 `SessionController` 内，是记录类型，用于承载 `Revoke All View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RevokeAllVO` is a record inside `SessionController` and carries the responsibility, state, or contract for `Revoke All View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RevokeAllVO` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RevokeAllVO` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param success 记录组件 `success` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `success` carries constructor data whose meaning is defined by the record contract.
     * @param revokedCount 记录组件 `revokedCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `revokedCount` carries constructor data whose meaning is defined by the record contract.
     */
    public record RevokeAllVO(/**
 * 字段 `success` 表示 `RevokeAllVO` 中与 `success` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `success` stores the `success`-related state, dependency, configuration, or result of `RevokeAllVO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `success` 时应保持 `RevokeAllVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `success`, preserve `RevokeAllVO`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean success, /**
 * 字段 `revokedCount` 表示 `RevokeAllVO` 中与 `revoked Count` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `revokedCount` stores the `revoked Count`-related state, dependency, configuration, or result of `RevokeAllVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `revokedCount` 时应保持 `RevokeAllVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `revokedCount`, preserve `RevokeAllVO`'s lifecycle, immutability, and thread-safety constraints.
 */ int revokedCount) {
    }

package top.egon.cola.platform.rbac3.admin.identity.domain.exception;

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
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import java.util.List;

/**
     * 类型 `IdentityMembershipNotFoundException` 位于 `InternalIdentityController` 内，是类型，用于承载 `Identity Membership Not Found Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IdentityMembershipNotFoundException` is a type inside `InternalIdentityController` and carries the responsibility, state, or contract for `Identity Membership Not Found Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IdentityMembershipNotFoundException` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IdentityMembershipNotFoundException` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public final class IdentityMembershipNotFoundException
            extends IllegalStateException {

        /**
         * 构造器 `IdentityMembershipNotFoundException` 用于创建并初始化 `IdentityMembershipNotFoundException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `IdentityMembershipNotFoundException` creates and initializes `IdentityMembershipNotFoundException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `IdentityMembershipNotFoundException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `IdentityMembershipNotFoundException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public IdentityMembershipNotFoundException(String identitySub, String tenantId) {
            super("active identity membership not found: identitySub="
                    + identitySub + ", tenantId=" + tenantId);
        }
    }

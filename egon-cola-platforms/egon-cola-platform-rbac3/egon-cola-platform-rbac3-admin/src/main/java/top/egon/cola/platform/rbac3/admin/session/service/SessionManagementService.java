package top.egon.cola.platform.rbac3.admin.session.service;

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
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionVO;
import top.egon.cola.platform.rbac3.admin.session.controller.SessionController;

/**
     * 类型 `SessionManagementService` 位于 `SessionController` 内，是接口，用于承载 `Session Management Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionManagementService` is an interface inside `SessionController` and carries the responsibility, state, or contract for `Session Management Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionManagementService` 作为 `SessionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionManagementService` as the responsibility boundary of `SessionController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface SessionManagementService {

        /**
         * 方法 `findByUser` 按照 `SessionManagementService` 的职责处理输入，完成 `find By User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findByUser` processes its inputs according to `SessionManagementService`'s responsibility, performs the `find By User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findByUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findByUser`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<SessionVO> findByUser(String tenantId, String userId);

        /**
         * 方法 `revoke` 按照 `SessionManagementService` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `revoke` processes its inputs according to `SessionManagementService`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        boolean revoke(String tenantId, String sessionId, Instant now);

        /**
         * 方法 `revokeAll` 按照 `SessionManagementService` 的职责处理输入，完成 `revoke All` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `revokeAll` processes its inputs according to `SessionManagementService`'s responsibility, performs the `revoke All` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `revokeAll` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `revokeAll`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        int revokeAll(String tenantId, String userId, Instant now);
    }

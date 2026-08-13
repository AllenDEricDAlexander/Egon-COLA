package top.egon.cola.platform.rbac3.admin.activation.repository;

import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.SessionStateVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationFacade;

/**
     * 类型 `ActivationTransaction` 位于 `RoleActivationFacade` 内，是接口，用于承载 `Activation Transaction` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivationTransaction` is an interface inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Activation Transaction`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivationTransaction` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivationTransaction` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ActivationTransaction {

        /**
         * 方法 `replace` 按照 `ActivationTransaction` 的职责处理输入，完成 `replace` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `replace` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `replace` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `replace` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `replace`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resolutionFactory 输入参数 `resolutionFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        TransactionResultVO replace(
                ReplaceCommandDTO command,
                Instant now,
                Function<SessionStateVO, ResolvedActivationVO> resolutionFactory);

        /**
         * 方法 `current` 按照 `ActivationTransaction` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `current` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        CurrentStateVO current(
                String tenantId,
                String identitySub,
                String userId,
                String sessionId,
                Instant now);

        /**
         * 方法 `markFenced` 按照 `ActivationTransaction` 的职责处理输入，完成 `mark Fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `markFenced` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `mark Fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `markFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `markFenced`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void markFenced(String mutationId, Instant now) {
        }

        /**
         * 方法 `markCompleted` 按照 `ActivationTransaction` 的职责处理输入，完成 `mark Completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `markCompleted` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `mark Completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `markCompleted` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `markCompleted`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void markCompleted(String mutationId, Instant now) {
        }

        /**
         * 方法 `markRecoveryRequired` 按照 `ActivationTransaction` 的职责处理输入，完成 `mark Recovery Required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `markRecoveryRequired` processes its inputs according to `ActivationTransaction`'s responsibility, performs the `mark Recovery Required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `markRecoveryRequired` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `markRecoveryRequired`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void markRecoveryRequired(
                String mutationId,
                String reasonCode,
                Instant now
        ) {
        }
    }

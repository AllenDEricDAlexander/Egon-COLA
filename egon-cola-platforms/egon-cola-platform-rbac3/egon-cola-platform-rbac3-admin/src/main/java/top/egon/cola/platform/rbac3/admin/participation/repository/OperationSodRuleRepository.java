package top.egon.cola.platform.rbac3.admin.participation.repository;

import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.core.participation.OperationSodSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.PriorActionRuleVO;

/**
     * 类型 `OperationSodRuleRepository` 位于 `ParticipationFacade` 内，是接口，用于承载 `Operation Sod Rule Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleRepository` is an interface inside `ParticipationFacade` and carries the responsibility, state, or contract for `Operation Sod Rule Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleRepository` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleRepository` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface OperationSodRuleRepository {
        /**
         * 方法 `rules` 按照 `OperationSodRuleRepository` 的职责处理输入，完成 `rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rules` processes its inputs according to `OperationSodRuleRepository`'s responsibility, performs the `rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param laterAction 输入参数 `laterAction`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param at 输入参数 `at`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<PriorActionRuleVO> rules(
                String tenantId,
                String applicationCode,
                String businessResource,
                String laterAction,
                Instant at);
    }

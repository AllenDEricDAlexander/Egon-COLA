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
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationRecordVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationFactVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.PriorActionRuleVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.AppendResultVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.dto.ConflictQueryDTO;

/**
     * 类型 `ParticipationRepository` 位于 `ParticipationFacade` 内，是接口，用于承载 `Participation Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ParticipationRepository` is an interface inside `ParticipationFacade` and carries the responsibility, state, or contract for `Participation Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ParticipationRepository` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ParticipationRepository` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ParticipationRepository {
        /**
         * 方法 `appendAtomically` 按照 `ParticipationRepository` 的职责处理输入，完成 `append Atomically` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `appendAtomically` processes its inputs according to `ParticipationRepository`'s responsibility, performs the `append Atomically` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `appendAtomically` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `appendAtomically`, then continue the business flow using its result, exception, or side effect.
         *
         * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rules 输入参数 `rules`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AppendResultVO appendAtomically(
                ParticipationRecordVO record,
                List<PriorActionRuleVO> rules);

        /**
         * 方法 `find` 按照 `ParticipationRepository` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `find` processes its inputs according to `ParticipationRepository`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
         *
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ParticipationFactVO> find(
                ConflictQueryDTO query,
                String tenantId,
                Instant lookbackFrom);
    }

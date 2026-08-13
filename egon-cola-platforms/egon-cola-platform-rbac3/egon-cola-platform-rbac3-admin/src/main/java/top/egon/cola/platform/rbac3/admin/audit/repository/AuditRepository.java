package top.egon.cola.platform.rbac3.admin.audit.repository;

import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import top.egon.cola.platform.rbac3.admin.audit.domain.dto.QueryDTO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditVO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditQueryPageVO;
import top.egon.cola.platform.rbac3.admin.audit.service.AuditQueryService;

/**
     * 类型 `AuditRepository` 位于 `AuditQueryService` 内，是接口，用于承载 `Audit Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditRepository` is an interface inside `AuditQueryService` and carries the responsibility, state, or contract for `Audit Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditRepository` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditRepository` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface AuditRepository {
        /**
         * 方法 `append` 按照 `AuditRepository` 的职责处理输入，完成 `append` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `append` processes its inputs according to `AuditRepository`'s responsibility, performs the `append` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `append` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `append`, then continue the business flow using its result, exception, or side effect.
         *
         * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AuditVO append(AuditVO record);

        /**
         * 方法 `query` 按照 `AuditRepository` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `query` processes its inputs according to `AuditRepository`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
         *
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AuditQueryPageVO query(QueryDTO query);
    }

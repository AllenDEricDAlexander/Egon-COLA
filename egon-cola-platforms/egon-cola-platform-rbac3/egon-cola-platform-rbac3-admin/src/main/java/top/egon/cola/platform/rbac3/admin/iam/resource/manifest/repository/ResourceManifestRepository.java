package top.egon.cola.platform.rbac3.admin.iam.resource.manifest.repository;

import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.vo.ActivationMutation;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.vo.StoredManifestVO;

/**
     * 类型 `ResourceManifestRepository` 位于 `ManifestFacade` 内，是接口，用于承载 `Manifest Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResourceManifestRepository` is an interface inside `ManifestFacade` and carries the responsibility, state, or contract for `Manifest Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResourceManifestRepository` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceManifestRepository` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ResourceManifestRepository {

        /**
         * 方法 `findByBuild` 按照 `ResourceManifestRepository` 的职责处理输入，完成 `find By Build` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findByBuild` processes its inputs according to `ResourceManifestRepository`'s responsibility, performs the `find By Build` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findByBuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findByBuild`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param artifactVersion 输入参数 `artifactVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param buildId 输入参数 `buildId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<StoredManifestVO> findByBuild(
                String tenantId,
                String applicationId,
                String artifactVersion,
                String buildId);

        /**
         * 方法 `insert` 按照 `ResourceManifestRepository` 的职责处理输入，完成 `insert` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `insert` processes its inputs according to `ResourceManifestRepository`'s responsibility, performs the `insert` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `insert` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `insert`, then continue the business flow using its result, exception, or side effect.
         *
         * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void insert(StoredManifestVO manifest);

        /**
         * 方法 `activate` 按照 `ResourceManifestRepository` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `activate` processes its inputs according to `ResourceManifestRepository`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedApplicationVersion 输入参数 `expectedApplicationVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedCurrentManifestVersion 输入参数 `expectedCurrentManifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedDefinitionSetId 输入参数 `expectedDefinitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default ActivationMutation activate(
                String tenantId,
                String applicationId,
                String manifestId,
                long expectedApplicationVersion,
                long expectedCurrentManifestVersion,
                String expectedDefinitionSetId,
                String actorId,
                String idempotencyKey,
                String reason,
                Instant now) {
            throw new UnsupportedOperationException("manifest activation is not configured");
        }

    }

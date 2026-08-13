package top.egon.cola.platform.rbac3.admin.resource.repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ApplicationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ResourceVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestValidationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestImpactVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ArchiveResultVO;
import top.egon.cola.platform.rbac3.admin.resource.service.ApplicationResourceFacade;

/**
     * 类型 `ApplicationResourceRepository` 位于 `ApplicationResourceFacade` 内，是接口，用于承载 `ApplicationResourceRepository` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ApplicationResourceRepository` is an interface inside `ApplicationResourceFacade` and carries the responsibility, state, or contract for `ApplicationResourceRepository`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ApplicationResourceRepository` 作为 `ApplicationResourceFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ApplicationResourceRepository` as the responsibility boundary of `ApplicationResourceFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ApplicationResourceRepository {

        /**
         * 方法 `applications` 按照 `ApplicationResourceRepository` 的职责处理输入，完成 `applications` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `applications` processes its inputs according to `ApplicationResourceRepository`'s responsibility, performs the `applications` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `applications` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `applications`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ApplicationVO> applications(String tenantId);

        /**
         * 方法 `resources` 按照 `ApplicationResourceRepository` 的职责处理输入，完成 `resources` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `resources` processes its inputs according to `ApplicationResourceRepository`'s responsibility, performs the `resources` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `resources` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `resources`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ResourceVO> resources(String tenantId, String applicationId);

        /**
         * 方法 `manifest` 按照 `ApplicationResourceRepository` 的职责处理输入，完成 `manifest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `manifest` processes its inputs according to `ApplicationResourceRepository`'s responsibility, performs the `manifest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `manifest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `manifest`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ManifestVO manifest(String tenantId, String manifestId);

        /**
         * 方法 `validation` 按照 `ApplicationResourceRepository` 的职责处理输入，完成 `validation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `validation` processes its inputs according to `ApplicationResourceRepository`'s responsibility, performs the `validation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `validation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `validation`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ManifestValidationVO validation(String tenantId, String manifestId);

        /**
         * 方法 `impact` 按照 `ApplicationResourceRepository` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `impact` processes its inputs according to `ApplicationResourceRepository`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ManifestImpactVO impact(String tenantId, String manifestId);

        /**
         * 方法 `archive` 按照 `ApplicationResourceRepository` 的职责处理输入，完成 `archive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `archive` processes its inputs according to `ApplicationResourceRepository`'s responsibility, performs the `archive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `archive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `archive`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ArchiveResultVO archive(
                String tenantId,
                String resourceId,
                long expectedVersion,
                String actorId,
                Instant now);
    }

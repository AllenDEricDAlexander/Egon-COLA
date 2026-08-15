package top.egon.cola.platform.rbac3.admin.iam.role.repository;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.CreateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.AssignPermissionCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.AssignPermissionsCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.RemovePermissionCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.dto.UpdateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleVO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleImpactVO;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleMutationResultVO;

/**
     * 类型 `RoleControlRepository` 位于 `RoleFacade` 内，是接口，用于承载 `Role Control Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleControlRepository` is an interface inside `RoleFacade` and carries the responsibility, state, or contract for `Role Control Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleControlRepository` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleControlRepository` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface RoleControlRepository {

        /**
         * 方法 `create` 按照 `RoleControlRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RoleMutationResultVO create(CreateRoleCommandDTO command, Instant now);

        /**
         * 方法 `assignPermission` 按照 `RoleControlRepository` 的职责处理输入，完成 `assign Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assignPermission` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `assign Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assignPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assignPermission`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RoleMutationResultVO assignPermission(AssignPermissionCommandDTO command, Instant now);

        /**
         * 方法 `assignPermissions` 按照 `RoleControlRepository` 的职责处理输入，完成 `assign Permissions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assignPermissions` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `assign Permissions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assignPermissions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assignPermissions`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleMutationResultVO assignPermissions(
                AssignPermissionsCommandDTO command,
                Instant now) {
            RoleMutationResultVO result = null;
            for (String permissionId : command.permissionIds()) {
                result = assignPermission(new AssignPermissionCommandDTO(
                        command.tenantId(),
                        command.applicationId(),
                        command.roleId(),
                        permissionId,
                        command.validFrom(),
                        command.validTo(),
                        command.actorId()), now);
            }
            return result;
        }

        /**
         * 方法 `removePermission` 按照 `RoleControlRepository` 的职责处理输入，完成 `remove Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `removePermission` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `remove Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `removePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `removePermission`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleMutationResultVO removePermission(
                RemovePermissionCommandDTO command,
                Instant now) {
            throw new UnsupportedOperationException("permission removal is not configured");
        }

        /**
         * 方法 `update` 按照 `RoleControlRepository` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `update` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleMutationResultVO update(UpdateRoleCommandDTO command, Instant now) {
            throw new UnsupportedOperationException("role update is not configured");
        }

        /**
         * 方法 `roles` 按照 `RoleControlRepository` 的职责处理输入，完成 `roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `roles` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `roles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `roles`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default List<RoleVO> roles(String tenantId, String applicationId) {
            throw new UnsupportedOperationException("role query is not configured");
        }

        /**
         * 方法 `impact` 按照 `RoleControlRepository` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `impact` processes its inputs according to `RoleControlRepository`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleImpactVO impact(String tenantId, String roleId) {
            throw new UnsupportedOperationException("role impact is not configured");
        }
    }

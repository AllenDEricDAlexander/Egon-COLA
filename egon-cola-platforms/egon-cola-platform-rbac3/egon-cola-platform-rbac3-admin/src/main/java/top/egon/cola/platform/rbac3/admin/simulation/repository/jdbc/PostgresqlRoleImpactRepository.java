package top.egon.cola.platform.rbac3.admin.simulation.repository.jdbc;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.repository.RoleImpactQuery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleImpactVO;
import top.egon.cola.platform.rbac3.admin.simulation.domain.vo.RoleImpactSnapshotVO;
import top.egon.cola.platform.rbac3.admin.simulation.repository.RoleImpactRepository;

/**
 * 类型 `PostgresqlRoleImpactRepository` 位于当前包内，是类型，用于承载 `Postgresql Role Impact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlRoleImpactRepository` is a type in its package and carries the responsibility, state, or contract for `Postgresql Role Impact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Loads role impact and the tenant policy version in one read-only transaction.
 */
@Repository
public class PostgresqlRoleImpactRepository
        implements RoleImpactRepository {

    /**
     * 字段 `roleFacade` 表示 `PostgresqlRoleImpactRepository` 中与 `role Facade` 相关的状态、依赖、配置或结果（声明类型 `RoleFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleFacade` stores the `role Facade`-related state, dependency, configuration, or result of `PostgresqlRoleImpactRepository` (declared type `RoleFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleFacade` 时应保持 `PostgresqlRoleImpactRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleFacade`, preserve `PostgresqlRoleImpactRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleImpactQuery roleImpactQuery;
    private final TenantAuthorizationStateRepository authorizationState;

    /**
     * 构造器 `PostgresqlRoleImpactRepository` 用于创建并初始化 `PostgresqlRoleImpactRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlRoleImpactRepository` creates and initializes `PostgresqlRoleImpactRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlRoleImpactRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlRoleImpactRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param roleFacade 输入参数 `roleFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authorizationState 输入参数 `authorizationState`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
    */
    public PostgresqlRoleImpactRepository(
            RoleImpactQuery roleImpactQuery,
            TenantAuthorizationStateRepository authorizationState) {
        this.roleImpactQuery = roleImpactQuery;
        this.authorizationState = authorizationState;
    }

    /**
     * 方法 `load` 按照 `PostgresqlRoleImpactRepository` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `PostgresqlRoleImpactRepository`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public RoleImpactSnapshotVO load(
            String tenantId,
            String roleId) {
        TenantAuthorizationStatePO state = authorizationState.requireForUpdate(
                Long.valueOf(tenantId));
        RoleImpactVO impact = roleImpactQuery.impact(tenantId, roleId);
        long policyVersion = state.getPolicyVersion();
        return new RoleImpactSnapshotVO(
                impact, policyVersion,
                checksum(tenantId, policyVersion, impact));
    }

    /**
     * 方法 `checksum` 按照 `PostgresqlRoleImpactRepository` 的职责处理输入，完成 `checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `checksum` processes its inputs according to `PostgresqlRoleImpactRepository`'s responsibility, performs the `checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `checksum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `checksum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param impact 输入参数 `impact`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String checksum(
            String tenantId,
            long policyVersion,
            RoleImpactVO impact) {
        String canonical = String.join("\n",
                tenantId,
                Long.toString(policyVersion),
                impact.roleId(),
                sorted(impact.activationRoots()),
                sorted(impact.roleFamily()),
                impact.effectiveFamilyRisk(),
                Long.toString(impact.permissionCount()),
                sorted(impact.conflicts()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    /**
     * 方法 `sorted` 按照 `PostgresqlRoleImpactRepository` 的职责处理输入，完成 `sorted` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sorted` processes its inputs according to `PostgresqlRoleImpactRepository`'s responsibility, performs the `sorted` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sorted` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sorted`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String sorted(List<String> values) {
        return values.stream().sorted().toList().toString();
    }
}

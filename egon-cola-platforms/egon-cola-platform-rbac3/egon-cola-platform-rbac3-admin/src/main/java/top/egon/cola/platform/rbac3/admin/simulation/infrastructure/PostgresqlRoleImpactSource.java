package top.egon.cola.platform.rbac3.admin.simulation.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 类型 `PostgresqlRoleImpactSource` 位于当前包内，是类型，用于承载 `Postgresql Role Impact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PostgresqlRoleImpactSource` is a type in its package and carries the responsibility, state, or contract for `Postgresql Role Impact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Loads role impact and the tenant policy version in one read-only transaction.
 */
@Repository
public class PostgresqlRoleImpactSource
        implements AuthorizationSimulationService.RoleImpactSource {

    /**
     * 字段 `entityManager` 表示 `PostgresqlRoleImpactSource` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `PostgresqlRoleImpactSource` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `PostgresqlRoleImpactSource` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `PostgresqlRoleImpactSource`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `roleFacade` 表示 `PostgresqlRoleImpactSource` 中与 `role Facade` 相关的状态、依赖、配置或结果（声明类型 `RoleFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleFacade` stores the `role Facade`-related state, dependency, configuration, or result of `PostgresqlRoleImpactSource` (declared type `RoleFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleFacade` 时应保持 `PostgresqlRoleImpactSource` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleFacade`, preserve `PostgresqlRoleImpactSource`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleFacade roleFacade;

    /**
     * 构造器 `PostgresqlRoleImpactSource` 用于创建并初始化 `PostgresqlRoleImpactSource` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PostgresqlRoleImpactSource` creates and initializes `PostgresqlRoleImpactSource`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PostgresqlRoleImpactSource` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PostgresqlRoleImpactSource`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleFacade 输入参数 `roleFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PostgresqlRoleImpactSource(
            EntityManager entityManager,
            RoleFacade roleFacade) {
        this.entityManager = entityManager;
        this.roleFacade = roleFacade;
    }

    /**
     * 方法 `load` 按照 `PostgresqlRoleImpactSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `PostgresqlRoleImpactSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public AuthorizationSimulationService.RoleImpactSnapshot load(
            String tenantId,
            String roleId) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId));
        if (tenant == null) {
            throw new IllegalArgumentException("tenant is missing");
        }
        RoleFacade.RoleImpactView impact = roleFacade.impact(tenantId, roleId);
        long policyVersion = tenant.getPolicyVersion();
        return new AuthorizationSimulationService.RoleImpactSnapshot(
                impact, policyVersion,
                checksum(tenantId, policyVersion, impact));
    }

    /**
     * 方法 `checksum` 按照 `PostgresqlRoleImpactSource` 的职责处理输入，完成 `checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `checksum` processes its inputs according to `PostgresqlRoleImpactSource`'s responsibility, performs the `checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            RoleFacade.RoleImpactView impact) {
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
     * 方法 `sorted` 按照 `PostgresqlRoleImpactSource` 的职责处理输入，完成 `sorted` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sorted` processes its inputs according to `PostgresqlRoleImpactSource`'s responsibility, performs the `sorted` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

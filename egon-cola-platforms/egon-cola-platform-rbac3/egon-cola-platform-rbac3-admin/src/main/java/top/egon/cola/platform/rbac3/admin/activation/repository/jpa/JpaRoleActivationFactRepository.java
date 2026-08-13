package top.egon.cola.platform.rbac3.admin.activation.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import top.egon.cola.platform.rbac3.admin.activation.repository.internal.MutableDsd;
import top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationFactRepository;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.ApplicationFactVO;

/**
 * 类型 `JpaRoleActivationFactRepository` 位于当前包内，是类型，用于承载 `Role Activation Fact Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JpaRoleActivationFactRepository` is a type in its package and carries the responsibility, state, or contract for `Role Activation Fact Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Loads one tenant-safe, database-time activation fact set.
 */
@Repository
public class JpaRoleActivationFactRepository
        implements RoleActivationFactRepository {

    /**
     * 字段 `entityManager` 表示 `JpaRoleActivationFactRepository` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `JpaRoleActivationFactRepository` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `JpaRoleActivationFactRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `JpaRoleActivationFactRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;

    /**
     * 构造器 `JpaRoleActivationFactRepository` 用于创建并初始化 `JpaRoleActivationFactRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JpaRoleActivationFactRepository` creates and initializes `JpaRoleActivationFactRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JpaRoleActivationFactRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JpaRoleActivationFactRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JpaRoleActivationFactRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 方法 `load` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    @Transactional(readOnly = true)
    public ActivationFactsVO load(
            String tenantId,
            String userId,
            Instant databaseNow
    ) {
        long tenant = Long.parseLong(tenantId);
        long user = Long.parseLong(userId);
        Object[] versions = one("""
                select u.auth_version, u.directory_snapshot_version, t.policy_version, u.status
                  from rbac3_user u
                  join rbac3_tenant t on t.id = u.tenant_id
                 where u.tenant_id = :tenantId and u.id = :userId
                """, Map.of("tenantId", tenant, "userId", user));
        if (!"ACTIVE".equals(text(versions[3]))) {
            throw new Rbac3RuleViolation("AUTHORIZATION_SUBJECT_INVALID");
        }

        List<Object[]> roleRows = rows("""
                select r.id, r.application_id, r.role_code, r.role_name,
                       r.status, r.risk_level, r.privileged,
                       landing.resource_code, r.landing_priority
                  from rbac3_role r
             left join rbac3_resource landing
                    on landing.tenant_id = r.tenant_id
                   and landing.application_id = r.application_id
                   and landing.id = r.landing_route_id
                 where r.tenant_id = :tenantId
                """, Map.of("tenantId", tenant));
        var nodes = new ArrayList<RoleNode>();
        var names = new TreeMap<String, String>();
        for (Object[] row : roleRows) {
            String roleId = text(row[0]);
            nodes.add(new RoleNode(
                    roleId,
                    text(row[1]),
                    text(row[2]),
                    "ACTIVE".equals(text(row[4])),
                    RoleNode.RiskLevel.valueOf(text(row[5])),
                    Boolean.TRUE.equals(row[6]),
                    nullableText(row[7]),
                    number(row[8]).intValue()));
            names.put(roleId, text(row[3]));
        }
        List<RoleEdge> edges = rows("""
                select senior_role_id, junior_role_id
                  from rbac3_role_inheritance
                 where tenant_id = :tenantId
                """, Map.of("tenantId", tenant)).stream()
                .map(row -> new RoleEdge(text(row[0]), text(row[1])))
                .toList();
        RoleHierarchy hierarchy = new RoleHierarchy(nodes, edges);

        List<EligibleAssignmentFact> assignments = rows("""
                select id, user_id, role_id, status, valid_from, valid_to
                  from rbac3_user_role_assignment
                 where tenant_id = :tenantId and user_id = :userId
                """, Map.of("tenantId", tenant, "userId", user)).stream()
                .map(row -> new EligibleAssignmentFact(
                        text(row[0]),
                        text(row[1]),
                        text(row[2]),
                        EligibleAssignmentFact.Status.valueOf(text(row[3])),
                        instant(row[4]),
                        row[5] == null ? null : instant(row[5])))
                .toList();

        Map<String, ApplicationFactVO> applications =
                new TreeMap<>();
        for (Object[] row : rows("""
                select id, application_code, application_name
                  from rbac3_application
                 where tenant_id = :tenantId and status = 'ACTIVE'
                """, Map.of("tenantId", tenant))) {
            String applicationId = text(row[0]);
            applications.put(applicationId,
                    new ApplicationFactVO(
                            applicationId, text(row[1]), text(row[2])));
        }

        return new ActivationFactsVO(
                tenantId,
                userId,
                hierarchy,
                assignments,
                dsdSets(tenant, databaseNow),
                authorizationFacts(tenant, user, databaseNow),
                number(versions[0]).longValue(),
                number(versions[2]).longValue(),
                "directory:" + number(versions[1]).longValue(),
                applications,
                names);
    }

    /**
     * 方法 `dsdSets` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `dsd Sets` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dsdSets` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `dsd Sets` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dsdSets` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dsdSets`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<DsdSetFact> dsdSets(long tenantId, Instant now) {
        Map<String, MutableDsd> values = new LinkedHashMap<>();
        for (Object[] row : rows("""
                select s.id, s.application_id, s.max_active_roles, m.role_id
                  from rbac3_sod_set s
                  join rbac3_sod_member m
                    on m.tenant_id = s.tenant_id and m.sod_set_id = s.id
                 where s.tenant_id = :tenantId
                   and s.constraint_type = 'DSD' and s.status = 'ACTIVE'
                   and s.valid_from <= :now
                   and (s.valid_to is null or s.valid_to > :now)
                 order by s.id, m.role_id
                """, Map.of("tenantId", tenantId, "now", now))) {
            String id = text(row[0]);
            MutableDsd set = values.computeIfAbsent(id, ignored -> new MutableDsd(
                    id, text(row[1]), number(row[2]).intValue()));
            set.addRoleId(text(row[3]));
        }
        return values.values().stream()
                .map(MutableDsd::toFact)
                .toList();
    }

    /**
     * 方法 `authorizationFacts` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `authorization Facts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationFacts` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `authorization Facts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationFacts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationFacts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuthorizationRuleFacts authorizationFacts(
            long tenantId,
            long userId,
            Instant now
    ) {
        List<AuthorizationRuleFacts.PermissionBinding> permissions = rows("""
                select rp.role_id, p.permission_code
                  from rbac3_role_permission rp
                  join rbac3_permission p
                    on p.tenant_id = rp.tenant_id and p.id = rp.permission_id
                 where rp.tenant_id = :tenantId
                   and rp.status = 'ACTIVE' and p.status = 'ACTIVE'
                   and rp.valid_from <= :now
                   and (rp.valid_to is null or rp.valid_to > :now)
                """, Map.of("tenantId", tenantId, "now", now)).stream()
                .map(row -> new AuthorizationRuleFacts.PermissionBinding(
                        text(row[0]), text(row[1])))
                .toList();

        var scopes = new ArrayList<AuthorizationRuleFacts.DataScopeFact>();
        for (Object[] row : rows("""
                select d.role_id, p.permission_code, d.scope_type,
                       ref.ref_type, ref.ref_id,
                       coalesce(d.directory_snapshot_version, u.directory_snapshot_version),
                       u.primary_org_unit_id
                  from rbac3_data_rule d
                  join rbac3_permission p
                    on p.tenant_id = d.tenant_id and p.id = d.permission_id
                  join rbac3_user u
                    on u.tenant_id = d.tenant_id and u.id = :userId
             left join rbac3_data_rule_ref ref
                    on ref.tenant_id = d.tenant_id and ref.data_rule_id = d.id
                 where d.tenant_id = :tenantId and d.status = 'ACTIVE'
                   and d.valid_from <= :now
                   and (d.valid_to is null or d.valid_to > :now)
                """, Map.of("tenantId", tenantId, "userId", userId, "now", now))) {
            String scopeType = text(row[2]);
            String dimension;
            String referenceId;
            if ("ALL".equals(scopeType)) {
                dimension = "TENANT_ALL";
                referenceId = null;
            } else if ("SELF".equals(scopeType)) {
                dimension = "USER";
                referenceId = Long.toString(userId);
            } else if (row[3] != null && row[4] != null) {
                dimension = switch (scopeType) {
                    case "ORG_TREE" -> "ORG_TREE";
                    case "DEPT_TREE" -> "DEPT_TREE";
                    default -> text(row[3]);
                };
                referenceId = text(row[4]);
            } else if (row[6] != null) {
                dimension = scopeType.startsWith("ORG") ? "ORG" : "DEPT";
                referenceId = text(row[6]);
            } else {
                dimension = "NONE";
                referenceId = null;
            }
            scopes.add(new AuthorizationRuleFacts.DataScopeFact(
                    text(row[0]), text(row[1]), dimension, referenceId,
                    number(row[5]).longValue()));
        }

        List<AuthorizationRuleFacts.FieldRuleFact> fieldRules = rows("""
                select f.role_id, resource.resource_code,
                       definition.field_code, f.access_level
                  from rbac3_field_rule f
                  join rbac3_field_definition definition
                    on definition.tenant_id = f.tenant_id
                   and definition.id = f.field_definition_id
                  join rbac3_resource resource
                    on resource.tenant_id = definition.tenant_id
                   and resource.id = definition.resource_id
                 where f.tenant_id = :tenantId and f.status = 'ACTIVE'
                   and definition.status = 'ACTIVE' and resource.status = 'ACTIVE'
                   and f.valid_from <= :now
                   and (f.valid_to is null or f.valid_to > :now)
                """, Map.of("tenantId", tenantId, "now", now)).stream()
                .map(row -> new AuthorizationRuleFacts.FieldRuleFact(
                        text(row[0]), text(row[1]), text(row[2]),
                        FieldAccessLevel.valueOf(text(row[3]))))
                .toList();

        List<AuthorizationRuleFacts.FieldDefinitionFact> definitions = rows("""
                select resource.resource_code, definition.field_code,
                       case when definition.writable then 'WRITE'
                            else definition.default_access end
                  from rbac3_field_definition definition
                  join rbac3_resource resource
                    on resource.tenant_id = definition.tenant_id
                   and resource.id = definition.resource_id
                 where definition.tenant_id = :tenantId
                   and definition.status = 'ACTIVE' and resource.status = 'ACTIVE'
                """, Map.of("tenantId", tenantId)).stream()
                .map(row -> new AuthorizationRuleFacts.FieldDefinitionFact(
                        text(row[0]), text(row[1]),
                        FieldAccessLevel.valueOf(text(row[2]))))
                .toList();

        List<AuthorizationRuleFacts.ResourceFact> resources = rows("""
                select resource.resource_code, permission.permission_code
                  from rbac3_resource resource
                  join rbac3_permission permission
                    on permission.tenant_id = resource.tenant_id
                   and permission.id = resource.required_permission_id
                 where resource.tenant_id = :tenantId
                   and resource.status = 'ACTIVE' and permission.status = 'ACTIVE'
                """, Map.of("tenantId", tenantId)).stream()
                .map(row -> new AuthorizationRuleFacts.ResourceFact(
                        text(row[0]), text(row[1])))
                .toList();

        List<AuthorizationRuleFacts.LandingRouteFact> landingRoutes = rows("""
                select role.id, route.resource_code, role.landing_priority,
                       permission.permission_code
                  from rbac3_role role
                  join rbac3_resource route
                    on route.tenant_id = role.tenant_id
                   and route.application_id = role.application_id
                   and route.id = role.landing_route_id
                  join rbac3_permission permission
                    on permission.tenant_id = route.tenant_id
                   and permission.id = route.required_permission_id
                 where role.tenant_id = :tenantId and role.status = 'ACTIVE'
                   and route.status = 'ACTIVE' and permission.status = 'ACTIVE'
                """, Map.of("tenantId", tenantId)).stream()
                .map(row -> new AuthorizationRuleFacts.LandingRouteFact(
                        text(row[0]), text(row[1]), number(row[2]).intValue(),
                        text(row[3])))
                .toList();

        return new AuthorizationRuleFacts(
                permissions, scopes, fieldRules, definitions, resources, landingRoutes);
    }

    /**
     * 方法 `rows` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `rows` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rows` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `rows` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rows` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rows`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sql 输入参数 `sql`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parameters 输入参数 `parameters`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Map<String, ?> parameters) {
        var query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return query.getResultList();
    }

    /**
     * 方法 `one` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `one` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `one` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `one` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `one` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `one`, then continue the business flow using its result, exception, or side effect.
     *
     * @param sql 输入参数 `sql`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parameters 输入参数 `parameters`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Object[] one(String sql, Map<String, ?> parameters) {
        List<Object[]> results = rows(sql, parameters);
        if (results.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return results.getFirst();
    }

    /**
     * 方法 `number` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `number` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `number` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `number` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `number` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `number`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Number number(Object value) {
        return (Number) value;
    }

    /**
     * 方法 `text` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `text` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `text` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `text`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String text(Object value) {
        return String.valueOf(value);
    }

    /**
     * 方法 `nullableText` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `nullable Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableText` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `nullable Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String nullableText(Object value) {
        return value == null ? null : text(value);
    }

    /**
     * 方法 `instant` 按照 `JpaRoleActivationFactRepository` 的职责处理输入，完成 `instant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `instant` processes its inputs according to `JpaRoleActivationFactRepository`'s responsibility, performs the `instant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `instant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `instant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Instant instant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalArgumentException("unsupported timestamp value: " + value);
    }

    }

package top.egon.cola.platform.rbac3.admin.activation.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
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

/**
 * Loads one tenant-safe, database-time activation fact set.
 */
@Repository
public class RoleActivationFactStore
        implements RoleActivationCandidateService.ActivationFactSource {

    private final EntityManager entityManager;

    public RoleActivationFactStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public RoleActivationCandidateService.ActivationFacts load(
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

        Map<String, RoleActivationCandidateService.ApplicationFact> applications =
                new TreeMap<>();
        for (Object[] row : rows("""
                select id, application_code, application_name
                  from rbac3_application
                 where tenant_id = :tenantId and status = 'ACTIVE'
                """, Map.of("tenantId", tenant))) {
            String applicationId = text(row[0]);
            applications.put(applicationId,
                    new RoleActivationCandidateService.ApplicationFact(
                            applicationId, text(row[1]), text(row[2])));
        }

        return new RoleActivationCandidateService.ActivationFacts(
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
            set.roleIds.add(text(row[3]));
        }
        return values.values().stream()
                .map(value -> new DsdSetFact(
                        value.id, value.applicationId,
                        value.maximumActive, value.roleIds))
                .toList();
    }

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

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Map<String, ?> parameters) {
        var query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return query.getResultList();
    }

    private Object[] one(String sql, Map<String, ?> parameters) {
        List<Object[]> results = rows(sql, parameters);
        if (results.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return results.getFirst();
    }

    private static Number number(Object value) {
        return (Number) value;
    }

    private static String text(Object value) {
        return String.valueOf(value);
    }

    private static String nullableText(Object value) {
        return value == null ? null : text(value);
    }

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

    private static final class MutableDsd {
        private final String id;
        private final String applicationId;
        private final int maximumActive;
        private final Set<String> roleIds = new TreeSet<>();

        private MutableDsd(String id, String applicationId, int maximumActive) {
            this.id = id;
            this.applicationId = applicationId;
            this.maximumActive = maximumActive;
        }
    }
}

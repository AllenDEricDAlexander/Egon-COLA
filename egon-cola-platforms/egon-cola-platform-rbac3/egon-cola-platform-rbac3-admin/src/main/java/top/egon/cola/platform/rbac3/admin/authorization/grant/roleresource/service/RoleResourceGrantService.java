package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.service;

import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantMutationVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantTreeVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.repository.ResourceApiBindingRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Coordinates resource-root reads and atomic direct-grant replacement. */
public class RoleResourceGrantService {

    private final RoleResourceGrantRepository grants;
    private final ResourceApiBindingRepository bindings;
    private final TenantAuthorizationStateRepository authorizationState;

    public RoleResourceGrantService(
            RoleResourceGrantRepository grants,
            ResourceApiBindingRepository bindings,
            TenantAuthorizationStateRepository authorizationState) {
        this.grants = Objects.requireNonNull(grants, "grants");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.authorizationState = Objects.requireNonNull(authorizationState, "authorizationState");
    }

    public RoleResourceGrantTreeVO tree(String tenantId, String roleId, Instant now) {
        Long tenant = positive(tenantId, "tenantId");
        Long role = positive(roleId, "roleId");
        RoleResourceGrantRepository.GrantTreeFacts facts = grants.loadTree(
                tenant, role, Objects.requireNonNull(now, "now"));
        Set<Long> direct = facts.directGrants().stream()
                .map(RoleResourceGrantPO::getResourceId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> derived = bindings.apiIdsForSources(facts.applicationId(), direct, now);
        Set<Long> derivedAndDirect = new java.util.HashSet<>(derived);
        derivedAndDirect.addAll(direct);
        Map<String, RoleResourceGrantTreeVO.Node> byCode = new java.util.TreeMap<>();
        for (RoleResourceGrantRepository.ResourceFact fact : facts.resources()) {
            boolean directGrant = direct.contains(fact.resourceId());
            boolean derivedGrant = derived.contains(fact.resourceId());
            boolean grantable = ("ROUTE".equals(fact.technicalType())
                    || "ACTION".equals(fact.technicalType())
                    || "API".equals(fact.technicalType()))
                    && "ACTIVE".equals(fact.status())
                    && "CONFIGURED".equals(fact.mappingStatus());
            String disabledReason = grantable ? null
                    : "CONFIGURED".equals(fact.mappingStatus()) ? "RESOURCE_NOT_GRANTABLE"
                    : "PERMISSION_MAPPING_REQUIRED";
            byCode.put(fact.resourceCode(), new RoleResourceGrantTreeVO.Node(
                    String.valueOf(fact.resourceId()), fact.resourceCode(), fact.resourceName(),
                    fact.category(), fact.technicalType(), fact.parentCode(), fact.status(),
                    fact.mappingStatus(), directGrant ? "DIRECT" : derivedGrant ? "DERIVED" : "NONE",
                    grantable, disabledReason,
                    fact.linkedApis().stream().map(api -> new RoleResourceGrantTreeVO.LinkedApi(
                            String.valueOf(api.resourceId()), api.resourceCode(), api.resourceName(),
                            api.method(), api.path())).toList(), List.of()));
        }
        List<RoleResourceGrantTreeVO.Node> roots = facts.resources().stream()
                .filter(fact -> fact.parentCode() == null || !byCode.containsKey(fact.parentCode()))
                .map(fact -> withChildren(fact.resourceCode(), byCode))
                .toList();
        long pages = facts.resources().stream().filter(fact -> "ROUTE".equals(fact.technicalType())).count();
        long actions = facts.resources().stream().filter(fact -> "ACTION".equals(fact.technicalType())).count();
        return new RoleResourceGrantTreeVO(
                role.toString(), facts.applicationId().toString(), facts.roleVersion(),
                ids(direct), ids(derived),
                new RoleResourceGrantTreeVO.Summary(pages, actions, derivedAndDirect.size(), 0L), roots);
    }

    @Transactional
    public RoleResourceGrantMutationVO replace(
            String tenantId,
            String roleId,
            ReplaceRoleResourcesRequestDTO request,
            String actorId,
            Instant now) {
        Long tenant = positive(tenantId, "tenantId");
        Long role = positive(roleId, "roleId");
        Objects.requireNonNull(request, "request");
        RoleResourceGrantRepository.GrantTreeFacts facts = grants.loadTree(tenant, role,
                Objects.requireNonNull(now, "now"));
        ReplaceRoleResourcesCommandDTO command = new ReplaceRoleResourcesCommandDTO(
                tenant, facts.applicationId(), role,
                request.resourceIds().stream().map(Long::valueOf).collect(
                        java.util.stream.Collectors.toUnmodifiableSet()),
                request.validFrom() == null ? now : request.validFrom(),
                request.validTo(), request.expectedRoleVersion(), actorId);
        return replace(command);
    }

    public RoleResourceGrantMutationVO replace(ReplaceRoleResourcesCommandDTO command) {
        Objects.requireNonNull(command, "command");
        RoleResourceGrantRepository.ReplaceResult result = grants.replace(
                new RoleResourceGrantRepository.ReplaceCommand(
                        command.tenantId(), command.applicationId(), command.roleId(),
                        command.resourceIds(), command.validFrom(), command.validTo(),
                        command.expectedRoleVersion(), command.actorId()));
        Set<Long> derived = bindings.apiIdsForSources(
                command.applicationId(), result.directResourceIds(), command.validFrom());
        Set<Long> effective = new LinkedHashSet<>(result.directResourceIds());
        effective.addAll(derived);
        long authVersion = result.addedCount() == 0L && result.removedCount() == 0L
                ? authorizationState.require(command.tenantId()).getPolicyVersion()
                : authorizationState.increment(command.tenantId(), command.actorId());
        long policyVersion = authorizationState.require(command.tenantId()).getPolicyVersion();
        return RoleResourceGrantMutationVO.success(
                command.roleId(), result.roleVersion(), result.directResourceIds(),
                derived, effective, result.addedCount(), result.removedCount(),
                effective.size(), authVersion, policyVersion);
    }

    private static RoleResourceGrantTreeVO.Node withChildren(
            String code,
            Map<String, RoleResourceGrantTreeVO.Node> byCode) {
        RoleResourceGrantTreeVO.Node current = byCode.get(code);
        List<RoleResourceGrantTreeVO.Node> children = byCode.values().stream()
                .filter(node -> code.equals(node.parentCode()))
                .map(node -> withChildren(node.resourceCode(), byCode))
                .toList();
        return new RoleResourceGrantTreeVO.Node(
                current.resourceId(), current.resourceCode(), current.name(), current.category(),
                current.technicalType(), current.parentCode(), current.status(),
                current.mappingStatus(), current.grantState(), current.grantable(),
                current.disabledReason(), current.linkedApis(), children);
    }

    private static List<String> ids(Set<Long> values) {
        return values.stream().sorted().map(String::valueOf).toList();
    }

    private static Long positive(String value, String fieldName) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(fieldName + " must be a positive decimal id");
        }
        return Long.valueOf(value);
    }
}

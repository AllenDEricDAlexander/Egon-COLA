package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.service;

import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantMutationVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantTreeVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.repository.ResourceApiBindingRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Coordinates resource-root reads and atomic direct-grant replacement. */
public final class RoleResourceGrantService {

    private final RoleResourceGrantRepository grants;
    private final ResourceApiBindingRepository bindings;

    public RoleResourceGrantService(
            RoleResourceGrantRepository grants,
            ResourceApiBindingRepository bindings) {
        this.grants = Objects.requireNonNull(grants, "grants");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
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
        return new RoleResourceGrantTreeVO(
                role.toString(), facts.applicationId().toString(), facts.roleVersion(),
                ids(direct), ids(derived),
                new RoleResourceGrantTreeVO.Summary(0L, 0L,
                        direct.size() + derived.size(), 0L), List.of());
    }

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
        return RoleResourceGrantMutationVO.success(
                command.roleId(), result.roleVersion(), result.directResourceIds(),
                derived, effective, result.directResourceIds().size(), 0L,
                effective.size(), 0L, 0L);
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

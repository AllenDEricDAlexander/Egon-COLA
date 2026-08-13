package top.egon.cola.platform.rbac3.admin.architecture;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;
import top.egon.cola.platform.rbac3.admin.auth.domain.po.ServiceCredentialPO;
import top.egon.cola.platform.rbac3.admin.auth.domain.po.ServicePermissionPO;
import top.egon.cola.platform.rbac3.admin.auth.domain.po.ServicePrincipalPO;
import top.egon.cola.platform.rbac3.admin.activation.domain.po.SessionActiveRolePO;
import top.egon.cola.platform.rbac3.admin.assignment.domain.po.AutoAssignmentRulePO;
import top.egon.cola.platform.rbac3.admin.assignment.domain.po.UserRoleAssignmentPO;
import top.egon.cola.platform.rbac3.admin.audit.domain.po.AuditLogPO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.DataRulePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.DataRuleReferencePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.FieldRulePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.OperationSodRulePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.RoleCardinalityPO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.RolePrerequisitePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.SodMemberPO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.SodSetPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.UserPositionSnapshotPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.ExternalIdentityPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserCredentialPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementOperationPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicyPO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementRolePO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementScopePO;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementSubjectPO;
import top.egon.cola.platform.rbac3.admin.participation.domain.po.BusinessParticipationPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ApplicationPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.FieldDefinitionPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.PermissionPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.PermissionResourcePO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ResourceManifestPO;
import top.egon.cola.platform.rbac3.admin.resource.domain.po.ResourcePO;
import top.egon.cola.platform.rbac3.admin.role.domain.po.RoleClosurePO;
import top.egon.cola.platform.rbac3.admin.role.domain.po.RoleInheritancePO;
import top.egon.cola.platform.rbac3.admin.role.domain.po.RolePermissionPO;
import top.egon.cola.platform.rbac3.admin.role.domain.po.RolePO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.po.AuthorizationMutationPO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.po.IdempotencyRecordPO;
import top.egon.cola.platform.rbac3.admin.session.domain.po.RefreshTokenPO;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 保护重命名持久化对象后的 JPQL 实体名及共享租户基类元数据。
 * Protects JPQL entity names and shared tenant base metadata after persistence-object renames.
 */
class JpaEntityNameCompatibilityTest {

    private static final Map<Class<?>, String> RENAMED_ENTITIES = Map.ofEntries(
            Map.entry(TenantPO.class, "TenantEntity"),
            Map.entry(ExternalIdentityPO.class, "ExternalIdentityEntity"),
            Map.entry(UserCredentialPO.class, "UserCredentialEntity"),
            Map.entry(UserPO.class, "UserEntity"),
            Map.entry(DirectorySnapshotPO.class, "DirectorySnapshotEntity"),
            Map.entry(OrgUnitPO.class, "OrgUnitEntity"),
            Map.entry(PositionPO.class, "PositionEntity"),
            Map.entry(UserPositionSnapshotPO.class, "UserPositionSnapshotEntity"),
            Map.entry(ServiceCredentialPO.class, "ServiceCredentialEntity"),
            Map.entry(ServicePermissionPO.class, "ServicePermissionEntity"),
            Map.entry(ServicePrincipalPO.class, "ServicePrincipalEntity"),
            Map.entry(SessionPO.class, "SessionEntity"),
            Map.entry(RefreshTokenPO.class, "RefreshTokenEntity"),
            Map.entry(ApplicationPO.class, "ApplicationEntity"),
            Map.entry(FieldDefinitionPO.class, "FieldDefinitionEntity"),
            Map.entry(PermissionPO.class, "PermissionEntity"),
            Map.entry(PermissionResourcePO.class, "PermissionResourceEntity"),
            Map.entry(ResourceManifestPO.class, "ResourceManifestEntity"),
            Map.entry(ResourcePO.class, "ResourceEntity"),
            Map.entry(RoleClosurePO.class, "RoleClosureEntity"),
            Map.entry(RoleInheritancePO.class, "RoleInheritanceEntity"),
            Map.entry(RolePermissionPO.class, "RolePermissionEntity"),
            Map.entry(RolePO.class, "RoleEntity"),
            Map.entry(SessionActiveRolePO.class, "SessionActiveRoleEntity"),
            Map.entry(AutoAssignmentRulePO.class, "AutoAssignmentRuleEntity"),
            Map.entry(UserRoleAssignmentPO.class, "UserRoleAssignmentEntity"),
            Map.entry(DataRulePO.class, "DataRuleEntity"),
            Map.entry(DataRuleReferencePO.class, "DataRuleReferenceEntity"),
            Map.entry(FieldRulePO.class, "FieldRuleEntity"),
            Map.entry(OperationSodRulePO.class, "OperationSodRuleEntity"),
            Map.entry(RoleCardinalityPO.class, "RoleCardinalityEntity"),
            Map.entry(RolePrerequisitePO.class, "RolePrerequisiteEntity"),
            Map.entry(SodMemberPO.class, "SodMemberEntity"),
            Map.entry(SodSetPO.class, "SodSetEntity"),
            Map.entry(ManagementOperationPO.class, "ManagementOperationEntity"),
            Map.entry(ManagementPolicyPO.class, "ManagementPolicyEntity"),
            Map.entry(ManagementRolePO.class, "ManagementRoleEntity"),
            Map.entry(ManagementScopePO.class, "ManagementScopeEntity"),
            Map.entry(ManagementSubjectPO.class, "ManagementSubjectEntity"),
            Map.entry(AuditLogPO.class, "AuditLogEntity"),
            Map.entry(BusinessParticipationPO.class, "BusinessParticipationEntity"),
            Map.entry(AuthorizationMutationPO.class, "AuthorizationMutationEntity"),
            Map.entry(IdempotencyRecordPO.class, "IdempotencyRecordEntity"));

    @Test
    void renamedEntitiesRetainTheirLegacyJpaNames() {
        RENAMED_ENTITIES.forEach((type, legacyName) -> {
            Entity entity = type.getAnnotation(Entity.class);
            assertThat(entity)
                    .as("@Entity on %s", type.getName())
                    .isNotNull();
            assertThat(entity.name())
                    .as("legacy JPQL entity name on %s", type.getName())
                    .isEqualTo(legacyName);
        });
    }

    @Test
    void tenantScopedPoRemainsAMappedSuperclass() {
        assertThat(TenantScopedPO.class)
                .hasAnnotation(MappedSuperclass.class);
    }
}

package top.egon.cola.platform.rbac3.admin.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rbac3_permission_resource")
public class PermissionResourceEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceEntity.ResourceType resourceType;

    @Column(name = "definition_set_id", length = 64)
    private String definitionSetId;

    @Column(name = "gateway_operation_id", length = 64)
    private String gatewayOperationId;

    @Column(name = "security_policy_id", length = 128)
    private String securityPolicyId;

    @Column(name = "mapping_version", nullable = false)
    private long mappingVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    protected PermissionResourceEntity() {
    }

    public PermissionResourceEntity(Long id, Long tenantId, Long applicationId, Long permissionId,
                                    Long resourceId, ResourceEntity.ResourceType resourceType,
                                    String definitionSetId, String gatewayOperationId,
                                    String securityPolicyId, long mappingVersion,
                                    String actorId, Instant now) {
        boolean apiIdentity = resourceType == ResourceEntity.ResourceType.API
                && present(definitionSetId) && present(gatewayOperationId);
        boolean nonApiIdentity = resourceType != ResourceEntity.ResourceType.API
                && definitionSetId == null && gatewayOperationId == null;
        if (!apiIdentity && !nonApiIdentity) {
            throw new IllegalArgumentException("invalid API operation identity");
        }
        if (mappingVersion < 0) {
            throw new IllegalArgumentException("mappingVersion must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.definitionSetId = definitionSetId;
        this.gatewayOperationId = gatewayOperationId;
        this.securityPolicyId = securityPolicyId;
        this.mappingVersion = mappingVersion;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    public enum Status {
        ACTIVE,
        STALE,
        DISABLED
    }
}

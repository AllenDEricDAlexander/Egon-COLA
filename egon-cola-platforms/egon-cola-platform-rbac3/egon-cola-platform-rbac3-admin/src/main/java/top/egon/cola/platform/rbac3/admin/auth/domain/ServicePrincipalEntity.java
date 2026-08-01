package top.egon.cola.platform.rbac3.admin.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "rbac3_service_principal")
public class ServicePrincipalEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "service_code", nullable = false, length = 128)
    private String serviceCode;

    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_envs", nullable = false, columnDefinition = "jsonb")
    private List<String> allowedEnvironments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_namespaces", nullable = false, columnDefinition = "jsonb")
    private List<String> allowedNamespaces;

    protected ServicePrincipalEntity() {
    }

    public ServicePrincipalEntity(
            Long id,
            Long tenantId,
            String serviceCode,
            String applicationCode,
            String displayName,
            List<String> allowedEnvironments,
            List<String> allowedNamespaces,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.serviceCode = required(serviceCode, "serviceCode");
        this.applicationCode = required(applicationCode, "applicationCode");
        this.displayName = required(displayName, "displayName");
        this.status = Status.ACTIVE;
        this.allowedEnvironments = List.copyOf(allowedEnvironments);
        this.allowedNamespaces = List.copyOf(allowedNamespaces);
        markCreated(actorId, now);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        ARCHIVED
    }
}

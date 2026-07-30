package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class TenantScopedEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Long getTenantId() {
        return tenantId;
    }

    protected void setTenantId(Long tenantId) {
        if (this.tenantId != null && !this.tenantId.equals(tenantId)) {
            throw new IllegalStateException("tenantId is immutable");
        }
        this.tenantId = tenantId;
    }

    public long getVersion() {
        return version;
    }
}

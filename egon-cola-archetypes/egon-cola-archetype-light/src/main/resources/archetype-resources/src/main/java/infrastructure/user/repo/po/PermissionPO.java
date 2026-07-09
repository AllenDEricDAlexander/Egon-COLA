package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "permissions")
public class PermissionPO {
    @Id
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PermissionPO() {
    }

    public PermissionPO(String code, String name, String status, Instant createdAt) {
        this.code = code;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}

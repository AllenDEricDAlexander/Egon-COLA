package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RolePermissionPO {
    @Id private Long id;
    @Column(name = "role_id", nullable = false) private Long roleId;
    @Column(name = "permission_id", nullable = false) private Long permissionId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public RolePermissionPO(Long id, Long roleId, Long permissionId, LocalDateTime createdAt) {
        this.id = id;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getRoleId() { return roleId; }
    public Long getPermissionId() { return permissionId; }
}

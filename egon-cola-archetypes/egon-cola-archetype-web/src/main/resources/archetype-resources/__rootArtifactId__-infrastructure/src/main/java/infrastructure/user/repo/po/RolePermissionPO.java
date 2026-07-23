package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "role_id", nullable = false, length = 64) private String roleId;
    @Column(name = "permission_id", nullable = false, length = 64) private String permissionId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public RolePermissionPO(String roleId, String permissionId, LocalDateTime createdAt) {
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public String getRoleId() { return roleId; }
    public String getPermissionId() { return permissionId; }
}

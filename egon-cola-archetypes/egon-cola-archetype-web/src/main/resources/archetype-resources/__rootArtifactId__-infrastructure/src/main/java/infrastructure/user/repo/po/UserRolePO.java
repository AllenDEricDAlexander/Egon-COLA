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
@Table(name = "user_roles", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRolePO {
    @Id @Column(length = 36) private String id;
    @Column(name = "user_id", nullable = false, length = 36) private String userId;
    @Column(name = "role_id", nullable = false, length = 36) private String roleId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public UserRolePO(String id, String userId, String roleId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRoleId() { return roleId; }
}

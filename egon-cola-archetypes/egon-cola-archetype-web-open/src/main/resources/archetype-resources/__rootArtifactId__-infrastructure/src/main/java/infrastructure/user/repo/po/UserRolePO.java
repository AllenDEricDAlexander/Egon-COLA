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
    @Id private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "role_id", nullable = false) private Long roleId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public UserRolePO(Long id, Long userId, Long roleId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getRoleId() { return roleId; }
}

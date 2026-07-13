package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}))
public class UserRolePO {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    @Column(name = "role_id", nullable = false, length = 64) private String roleId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected UserRolePO() {}
    public UserRolePO(String userId, String roleId, LocalDateTime createdAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getRoleId() { return roleId; }
}

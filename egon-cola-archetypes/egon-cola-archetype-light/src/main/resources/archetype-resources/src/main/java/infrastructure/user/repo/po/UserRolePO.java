package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRolePO.Key.class)
public class UserRolePO {
    @Id
    @Column(name = "user_id")
    private String userId;
    @Id
    @Column(name = "role_code")
    private String roleCode;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRolePO() {
    }

    public UserRolePO(String userId, String roleCode, Instant assignedAt) {
        this.userId = userId;
        this.roleCode = roleCode;
        this.assignedAt = assignedAt;
    }

    public String getUserId() { return userId; }
    public String getRoleCode() { return roleCode; }
    public Instant getAssignedAt() { return assignedAt; }

    public static final class Key implements Serializable {
        private String userId;
        private String roleCode;

        public Key() {
        }

        public Key(String userId, String roleCode) {
            this.userId = userId;
            this.roleCode = roleCode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(roleCode, key.roleCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleCode);
        }
    }
}

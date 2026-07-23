package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRolePO.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserRolePO {
    @Id
    @Column(name = "user_id")
    private String userId;
    @Id
    @Column(name = "role_code")
    private String roleCode;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    public String getUserId() { return userId; }
    public String getRoleCode() { return roleCode; }
    public Instant getAssignedAt() { return assignedAt; }

    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Key implements Serializable {
        private String userId;
        private String roleCode;

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

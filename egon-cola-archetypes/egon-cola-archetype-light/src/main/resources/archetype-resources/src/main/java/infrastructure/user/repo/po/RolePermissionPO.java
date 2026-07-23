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
@Table(name = "role_permissions")
@IdClass(RolePermissionPO.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RolePermissionPO {
    @Id
    @Column(name = "role_code")
    private String roleCode;
    @Id
    @Column(name = "permission_code")
    private String permissionCode;
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    public String getRoleCode() { return roleCode; }
    public String getPermissionCode() { return permissionCode; }
    public Instant getGrantedAt() { return grantedAt; }

    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Key implements Serializable {
        private String roleCode;
        private String permissionCode;

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(roleCode, key.roleCode)
                    && Objects.equals(permissionCode, key.permissionCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleCode, permissionCode);
        }
    }
}

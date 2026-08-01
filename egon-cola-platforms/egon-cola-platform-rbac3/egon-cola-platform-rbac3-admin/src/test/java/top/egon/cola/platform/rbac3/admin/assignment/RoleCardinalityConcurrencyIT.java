package top.egon.cola.platform.rbac3.admin.assignment;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.assignment.infrastructure.PostgresqlAssignmentLockStore;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCardinalityConcurrencyIT {

    @Test
    void canonicalCapacityLockKeyIsStableAndScopeSensitive() {
        String first = PostgresqlAssignmentLockStore.canonicalKey(
                "10001", "root-1", "DEPT", "90001");
        String same = PostgresqlAssignmentLockStore.canonicalKey(
                "10001", "root-1", "DEPT", "90001");
        String different = PostgresqlAssignmentLockStore.canonicalKey(
                "10001", "root-1", "DEPT", "90002");

        assertThat(first).isEqualTo(same).isNotEqualTo(different);
        assertThat(PostgresqlAssignmentLockStore.advisoryLockId(first)).isPositive();
    }
}

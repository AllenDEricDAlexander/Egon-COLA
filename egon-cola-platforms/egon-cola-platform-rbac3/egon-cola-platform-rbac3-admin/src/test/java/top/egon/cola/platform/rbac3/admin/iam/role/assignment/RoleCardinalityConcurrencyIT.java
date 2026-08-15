package top.egon.cola.platform.rbac3.admin.iam.role.assignment;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.repository.jdbc.PostgresqlAssignmentLockRepository;

import static org.assertj.core.api.Assertions.assertThat;

class RoleCardinalityConcurrencyIT {

    @Test
    void canonicalCapacityLockKeyIsStableAndScopeSensitive() {
        String first = PostgresqlAssignmentLockRepository.canonicalKey(
                "10001", "root-1", "DEPT", "90001");
        String same = PostgresqlAssignmentLockRepository.canonicalKey(
                "10001", "root-1", "DEPT", "90001");
        String different = PostgresqlAssignmentLockRepository.canonicalKey(
                "10001", "root-1", "DEPT", "90002");

        assertThat(first).isEqualTo(same).isNotEqualTo(different);
        assertThat(PostgresqlAssignmentLockRepository.advisoryLockId(first)).isPositive();
    }
}

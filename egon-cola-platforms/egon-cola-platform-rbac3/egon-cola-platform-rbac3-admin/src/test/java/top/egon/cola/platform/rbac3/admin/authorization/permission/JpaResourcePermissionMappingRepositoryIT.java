package top.egon.cola.platform.rbac3.admin.authorization.permission;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.rbac3.admin.authorization.permission.repository.jpa.JpaResourcePermissionMappingRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaResourcePermissionMappingRepositoryIT {

    @Test
    void repositoryIsTheJPAAdapterForAdminMappingFacts() {
        assertTrue(JpaResourcePermissionMappingRepository.class
                .isAnnotationPresent(Repository.class));
    }
}

package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.jpa.JpaRoleResourceGrantRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaRoleResourceGrantRepositoryIT {

    @Test
    void repositoryIsTheJPAAdapterForRoleResourceFacts() {
        assertTrue(JpaRoleResourceGrantRepository.class
                .isAnnotationPresent(Repository.class));
    }
}

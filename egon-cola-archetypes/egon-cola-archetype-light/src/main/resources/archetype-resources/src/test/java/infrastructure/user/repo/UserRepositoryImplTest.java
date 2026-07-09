package ${package}.infrastructure.user.repo;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.JpaTestApplication;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.impl.PermissionRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.RoleRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.UserRepositoryImpl;
import ${package}.infrastructure.user.repo.jpa.RolePermissionJpaRepository;
import ${package}.infrastructure.user.repo.jpa.UserRoleJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles({"test", "jpa-test"})
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({
        UserRepositoryImpl.class,
        RoleRepositoryImpl.class,
        PermissionRepositoryImpl.class,
        UserPOConverter.class,
        RolePOConverter.class,
        PermissionPOConverter.class
})
class UserRepositoryImplTest {
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;
    @Autowired UserRoleJpaRepository userRoleJpaRepository;
    @Autowired RolePermissionJpaRepository rolePermissionJpaRepository;
    @Autowired EntityManager entityManager;

    @Test
    void persists_user_role_and_permission_aggregates() {
        User user = user("u-1", "mario@example.com");
        Role role = new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE);
        Permission permission = new Permission(
                new PermissionCode("course:read"), "Read courses", PermissionStatus.ACTIVE);
        userRepository.save(user);
        roleRepository.save(role);
        permissionRepository.save(permission);
        UserAggregate userAggregate = new UserAggregate(user);
        userAggregate.assign(role);
        userRepository.saveRoles(userAggregate);
        RolePermissionAggregate roleAggregate = new RolePermissionAggregate(role);
        roleAggregate.grant(permission);
        roleRepository.savePermissions(roleAggregate);
        entityManager.flush();
        entityManager.clear();

        assertEquals("Mario", userRepository.findById(new UserId("u-1")).orElseThrow().name());
        assertEquals("course:read", permissionRepository.findByUserId(new UserId("u-1"))
                .getFirst().code().value());
        assertEquals(1, userRoleJpaRepository.count());
        assertEquals(1, rolePermissionJpaRepository.count());
    }

    @Test
    void rejects_duplicate_email() {
        userRepository.save(user("u-1", "mario@example.com"));
        userRepository.save(user("u-2", "mario@example.com"));

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    private User user(String id, String email) {
        return new User(new UserId(id), "ext-" + id, "Mario", email, UserStatus.ACTIVE);
    }
}

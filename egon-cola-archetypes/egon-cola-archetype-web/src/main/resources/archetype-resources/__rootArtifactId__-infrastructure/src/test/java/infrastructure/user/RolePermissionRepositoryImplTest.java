package ${package}.infrastructure.user;

import ${package}.domain.aggregates.user.RolePermissionAggregate;
import ${package}.domain.aggregates.user.UserAggregate;
import ${package}.domain.entities.user.Permission;
import ${package}.domain.entities.user.Role;
import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.repos.user.PermissionRepository;
import ${package}.domain.repos.user.RoleRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;
import ${package}.infrastructure.repo.user.converter.PermissionPOConverter;
import ${package}.infrastructure.repo.user.converter.RolePOConverter;
import ${package}.infrastructure.repo.user.converter.UserPOConverter;
import ${package}.infrastructure.repo.user.impl.PermissionRepositoryImpl;
import ${package}.infrastructure.repo.user.impl.RoleRepositoryImpl;
import ${package}.infrastructure.repo.user.impl.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:role-permission-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Import({UserRepositoryImpl.class, RoleRepositoryImpl.class, PermissionRepositoryImpl.class,
    UserPOConverter.class, RolePOConverter.class, PermissionPOConverter.class})
@ContextConfiguration(classes = RolePermissionRepositoryImplTest.TestConfiguration.class)
class RolePermissionRepositoryImplTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;

    @Test
    void persistsRoleAssignmentAndPermissionGrantRelations() {
        User user = userRepository.save(new User(
            new UserId("user-role-1"), "Mario", "role@example.com", UserStatus.ACTIVE, List.of()));
        Role role = roleRepository.findByCode(new RoleCode("STUDENT")).orElseThrow();
        Permission permission = permissionRepository.findByCode(new PermissionCode("CLASS_READ")).orElseThrow();

        UserAggregate userAggregate = new UserAggregate(user);
        userAggregate.assignRole(role);
        userRepository.save(userAggregate.user());
        RolePermissionAggregate permissionAggregate = new RolePermissionAggregate(role, role.permissionCodes());
        permissionAggregate.grant(permission);
        roleRepository.save(permissionAggregate.role());

        assertThat(userRepository.findById(user.id())).get()
            .extracting(User::roleCodes).isEqualTo(List.of(new RoleCode("STUDENT")));
        assertThat(permissionRepository.findByUserId(user.id()))
            .extracting(Permission::code).containsExactly(new PermissionCode("CLASS_READ"));
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackages = "${package}.infrastructure.repo")
    @EnableJpaRepositories(basePackages = "${package}.infrastructure.repo")
    static class TestConfiguration {
    }
}

package ${package}.infrastructure.user.repo;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.PermissionType;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.impl.PermissionRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.RoleRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.UserRepositoryImpl;
import ${package}.infrastructure.user.repo.jpa.RolePermissionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:role-permission-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({UserRepositoryImpl.class, RoleRepositoryImpl.class, PermissionRepositoryImpl.class,
    UserPOConverter.class, RolePOConverter.class, PermissionPOConverter.class,
    UuidV7Generator.class})
@ContextConfiguration(classes = RolePermissionRepositoryImplTest.TestConfiguration.class)
class RolePermissionRepositoryImplTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;
    @Autowired RolePermissionJpaRepository rolePermissionJpaRepository;

    @Test
    void persistsRoleAssignmentAndPermissionGrantRelations() {
        UuidV7Generator idGenerator = new UuidV7Generator();
        String userId = new UuidV7Generator().nextId();
        User user = userRepository.save(new User(
            new UserId(userId), "Mario", "role@example.com", UserStatus.ACTIVE, List.of()));
        Role role = roleRepository.save(new Role(
            idGenerator.nextId(), new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE));
        Permission permission = permissionRepository.save(new Permission(
            idGenerator.nextId(),
            new PermissionCode("CLASS_READ"),
            "Read school class",
            PermissionType.API,
            PermissionStatus.ACTIVE));

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
        String relationId = rolePermissionJpaRepository
                .findByRoleId(roleRepository.findByCode(new RoleCode("STUDENT")).orElseThrow()
                        .id())
                .getFirst()
                .getId();
        assertThat(relationId).hasSize(36);
        assertThat(UUID.fromString(relationId).version()).isEqualTo(7);
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackages = {
            "${package}.infrastructure.user.repo.po",
            "${package}.infrastructure.teaching.repo.po"
    })
    @EnableJpaRepositories(basePackages = {
            "${package}.infrastructure.user.repo.jpa",
            "${package}.infrastructure.teaching.repo.jpa"
    })
    static class TestConfiguration {
    }
}

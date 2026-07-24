package ${package}.infrastructure.user.repo;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.impl.UserRepositoryImpl;
import ${package}.infrastructure.user.repo.jpa.UserRoleJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/default",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Import({UserRepositoryImpl.class, UserPOConverter.class, UuidV7Generator.class})
@ContextConfiguration(classes = UserRepositoryImplTest.TestConfiguration.class)
class UserRepositoryImplTest {

    @Autowired
    private UserRepositoryImpl repository;
    @Autowired
    private UserRoleJpaRepository userRoleJpaRepository;

    @Test
    void savesAndRestoresNormalizedUser() {
        User saved = repository.save(
            new User(
                    new UserId(new UuidV7Generator().nextId()),
                    "Mario",
                    "mario@example.com",
                    UserStatus.ACTIVE,
                    List.of(new RoleCode("STUDENT"))));

        assertThat(repository.findById(saved.id())).get()
            .extracting(User::email, User::status)
            .containsExactly("mario@example.com", UserStatus.ACTIVE);
        String relationId = userRoleJpaRepository.findByUserId(saved.id().value()).getFirst().getId();
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

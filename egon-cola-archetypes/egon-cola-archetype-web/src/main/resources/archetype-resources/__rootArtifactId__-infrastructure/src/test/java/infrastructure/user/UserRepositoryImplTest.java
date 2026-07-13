package ${package}.infrastructure.user;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.repo.user.converter.UserPOConverter;
import ${package}.infrastructure.repo.user.impl.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Import({UserRepositoryImpl.class, UserPOConverter.class})
@ContextConfiguration(classes = UserRepositoryImplTest.TestConfiguration.class)
class UserRepositoryImplTest {

    @Autowired
    private UserRepositoryImpl repository;

    @Test
    void savesAndRestoresNormalizedUser() {
        User saved = repository.save(
            new User(new UserId("user-1"), "Mario", "mario@example.com", UserStatus.ACTIVE));

        assertThat(repository.findById(saved.id())).get()
            .extracting(User::email, User::status)
            .containsExactly("mario@example.com", UserStatus.ACTIVE);
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackages = "${package}.infrastructure.repo")
    @EnableJpaRepositories(basePackages = "${package}.infrastructure.repo")
    static class TestConfiguration {
    }
}

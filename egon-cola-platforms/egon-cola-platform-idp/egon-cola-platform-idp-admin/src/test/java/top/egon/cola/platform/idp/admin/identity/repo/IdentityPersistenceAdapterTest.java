package top.egon.cola.platform.idp.admin.identity.repo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.platform.idp.admin.identity.domain.pojo.IdentityCredentialEntity;
import top.egon.cola.platform.idp.admin.identity.domain.pojo.IdentityUserEntity;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:idp-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@ContextConfiguration(classes = IdentityPersistenceAdapterTest.TestApplication.class)
@Import(IdentityPersistenceAdapter.class)
class IdentityPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final IdentityPersistenceAdapter adapter;

    @Autowired
    IdentityPersistenceAdapterTest(IdentityPersistenceAdapter adapter) {
        this.adapter = adapter;
    }

    @Test
    void persistsAndFindsGlobalUserAndCredential() {
        IdentityUser user = user(0L, 0L);
        adapter.save(user, 0L);
        adapter.save(credential(0L), 0L);

        assertEquals(user, adapter.findById("1001").orElseThrow());
        assertEquals(user, adapter.findByNormalizedUsername("alice")
                .orElseThrow());
        assertEquals(credential(0L), adapter.findActive("1001")
                .orElseThrow());
    }

    @Test
    void rejectsStaleExpectedVersion() {
        adapter.save(user(0L, 0L), 0L);
        adapter.save(user(1L, 1L), 0L);

        OptimisticLockingFailureException exception = assertThrows(
                OptimisticLockingFailureException.class,
                () -> adapter.save(user(2L, 2L), 0L)
        );
        assertTrue(exception.getMessage().contains("1001"));
    }

    private IdentityUser user(long tokenVersion, long version) {
        return new IdentityUser(
                "1001",
                "Alice",
                "alice",
                "Alice",
                IdentityUserStatus.ACTIVE,
                tokenVersion,
                0,
                null,
                null,
                version
        );
    }

    private PasswordCredential credential(long version) {
        return new PasswordCredential(
                "1001",
                "{argon2}encoded",
                NOW,
                false,
                PasswordCredential.Status.ACTIVE,
                version
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            IdentityUserEntity.class,
            IdentityCredentialEntity.class
    })
    static class TestApplication {
    }
}

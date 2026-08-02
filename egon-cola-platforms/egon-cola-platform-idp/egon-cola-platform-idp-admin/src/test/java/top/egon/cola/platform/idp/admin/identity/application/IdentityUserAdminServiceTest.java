package top.egon.cola.platform.idp.admin.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityUserAdminServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final FakePersistence persistence = new FakePersistence();
    private final FakeState state = new FakeState();
    private final List<IdentitySecurityEvent> events = new ArrayList<>();
    private IdentityUserAdminService service;

    @BeforeEach
    void setUp() {
        service = new IdentityUserAdminService(
                persistence,
                persistence,
                persistence,
                new FakePasswordHash(),
                state,
                events::add,
                () -> 1001L,
                new UsernameNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "TempPassword12345"
        );
    }

    @Test
    void createsNormalizedUserWithOneTimePasswordAndStateProjection() {
        IdentityUserAdminService.CreatedUserView created = service.create(
                new IdentityUserAdminService.CreateUserCommand(
                        " Alice ",
                        "Alice Zhang"
                )
        );

        assertThat(created.subject()).isEqualTo("1001");
        assertThat(created.oneTimePassword()).isEqualTo("TempPassword12345");
        assertThat(persistence.users.get("1001").normalizedUsername())
                .isEqualTo("alice");
        assertThat(persistence.credentials.get("1001").mustChangePassword())
                .isTrue();
        assertThat(state.states).extracting(IdentityUserState::subject)
                .containsExactly("1001");
        assertThat(events).extracting(IdentitySecurityEvent::eventType)
                .containsExactly("IDENTITY_USER_CREATED");
    }

    @Test
    void disablingAndPasswordResetBumpTokenVersionAndRevokeFamilies() {
        service.create(new IdentityUserAdminService.CreateUserCommand(
                "alice",
                "Alice"
        ));

        IdentityUserAdminService.UserView disabled = service.update(
                "1001",
                new IdentityUserAdminService.UpdateUserCommand(
                        "Alice Disabled",
                        IdentityUserStatus.DISABLED,
                        0L
                )
        );
        IdentityUserAdminService.ResetPasswordView reset =
                service.resetPassword("1001");

        assertThat(disabled.tokenVersion()).isEqualTo(1L);
        assertThat(reset.oneTimePassword()).isEqualTo("TempPassword12345");
        assertThat(persistence.users.get("1001").tokenVersion())
                .isEqualTo(2L);
        assertThat(persistence.credentials.get("1001").mustChangePassword())
                .isTrue();
        assertThat(state.revocations)
                .containsExactly("1001:1:USER_DISABLED", "1001:2:PASSWORD_RESET");
    }

    private static final class FakePersistence
            implements IdentityUserStore, PasswordCredentialStore,
            IdentityUserAdminService.UserDirectory {

        private final Map<String, IdentityUser> users = new HashMap<>();
        private final Map<String, PasswordCredential> credentials =
                new HashMap<>();

        @Override
        public Optional<IdentityUser> findByNormalizedUsername(String value) {
            return users.values().stream()
                    .filter(user -> user.normalizedUsername().equals(value))
                    .findFirst();
        }

        @Override
        public Optional<IdentityUser> findById(String identitySub) {
            return Optional.ofNullable(users.get(identitySub));
        }

        @Override
        public IdentityUser save(IdentityUser user, long expectedVersion) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<PasswordCredential> findActive(String identitySub) {
            return Optional.ofNullable(credentials.get(identitySub));
        }

        @Override
        public PasswordCredential save(
                PasswordCredential credential,
                long expectedVersion
        ) {
            credentials.put(credential.identitySub(), credential);
            return credential;
        }

        @Override
        public List<IdentityUser> list() {
            return List.copyOf(users.values());
        }
    }

    private static final class FakePasswordHash implements PasswordHashPort {

        @Override
        public boolean matches(char[] rawPassword, String encodedPassword) {
            return false;
        }

        @Override
        public String encode(char[] rawPassword) {
            return "hash:" + new String(rawPassword);
        }

        @Override
        public String dummyHash() {
            return "dummy";
        }

        @Override
        public boolean needsUpgrade(String encodedPassword) {
            return false;
        }
    }

    private static final class FakeState implements IdentityUserStatePort {

        private final List<IdentityUserState> states = new ArrayList<>();
        private final List<String> revocations = new ArrayList<>();

        @Override
        public void publish(IdentityUserState value) {
            states.add(value);
        }

        @Override
        public void revokeFamilies(
                String identitySub,
                long tokenVersion,
                String reason
        ) {
            revocations.add(identitySub + ':' + tokenVersion + ':' + reason);
        }
    }
}

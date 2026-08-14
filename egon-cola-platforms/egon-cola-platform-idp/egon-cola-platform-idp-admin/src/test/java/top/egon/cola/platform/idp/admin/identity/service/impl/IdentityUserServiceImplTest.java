package top.egon.cola.platform.idp.admin.identity.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.identity.domain.dto.CreateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.dto.UpdateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.ResetPasswordVO;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.RefreshTokenRecord;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityUserServiceImplTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final FakePersistence persistence = new FakePersistence();
    private final FakeState state = new FakeState();
    private final FakeRefreshTokens refreshTokens = new FakeRefreshTokens();
    private final List<IdentitySecurityEvent> events = new ArrayList<>();
    private IdentityUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new IdentityUserServiceImpl(
                persistence,
                persistence,
                persistence,
                new FakePasswordHash(),
                state,
                events::add,
                refreshTokens,
                () -> 1001L,
                new UsernameNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "TempPassword12345"
        );
    }

    @Test
    void createsNormalizedUserWithOneTimePasswordAndStateProjection() {
        CreatedIdentityUserVO created = service.create(
                new CreateIdentityUserDTO(
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
    void disablingAndPasswordResetRevokeRefreshTokens() {
        service.create(new CreateIdentityUserDTO(
                "alice",
                "Alice"
        ));

        IdentityUserVO disabled = service.update(
                "1001",
                new UpdateIdentityUserDTO(
                        "Alice Disabled",
                        IdentityUserStatus.DISABLED,
                        0L
                )
        );
        ResetPasswordVO reset =
                service.resetPassword("1001");

        assertThat(disabled.version()).isEqualTo(1L);
        assertThat(reset.oneTimePassword()).isEqualTo("TempPassword12345");
        assertThat(persistence.users.get("1001").version())
                .isEqualTo(2L);
        assertThat(persistence.credentials.get("1001").mustChangePassword())
                .isTrue();
        assertThat(refreshTokens.revocations)
                .containsExactly("1001:USER_DISABLED", "1001:PASSWORD_RESET");
    }

    private static final class FakePersistence
            implements IdentityUserStore, PasswordCredentialStore,
            IdentityUserDirectory {

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

        @Override
        public void publish(IdentityUserState value) {
            states.add(value);
        }
    }

    private static final class FakeRefreshTokens implements RefreshTokenStore {

        private final List<String> revocations = new ArrayList<>();

        @Override
        public void create(RefreshTokenRecord record) {
        }

        @Override
        public java.util.Optional<RefreshTokenRecord> findValid(
                String tokenDigest,
                Instant now
        ) {
            return java.util.Optional.empty();
        }

        @Override
        public void revokeToken(String tokenDigest, String reason, Instant now) {
        }

        @Override
        public void revokeSubject(String identitySub, String reason, Instant now) {
            revocations.add(identitySub + ':' + reason);
        }

        @Override
        public void expire(Instant now) {
        }
    }
}

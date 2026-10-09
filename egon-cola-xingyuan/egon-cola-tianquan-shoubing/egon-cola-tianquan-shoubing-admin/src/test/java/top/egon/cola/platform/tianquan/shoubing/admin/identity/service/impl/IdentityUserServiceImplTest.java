package top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.dto.CreateIdentityUserDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.dto.UpdateIdentityUserDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.vo.ResetPasswordVO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthSubjectRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityUserState;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStatePort;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IdentityUserServiceImplTest {

    @Test
    void identityServiceWiresTheSharedRevocationService() {
        assertThatCode(() -> Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.TokenRevocationServiceImpl"))
                .doesNotThrowAnyException();
        assertThat(java.util.Arrays.stream(IdentityUserServiceImpl.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .contains("revocations").doesNotContain("refreshTokens");
    }

    @Test
    void requiredArgsConstructorCarriesQualifiersForEveryInjectedServicePort() {
        var constructor = IdentityUserServiceImpl.class.getDeclaredConstructors()[0];
        assertThat(Arrays.stream(constructor.getParameterAnnotations())
                .allMatch(annotations -> Arrays.stream(annotations)
                        .anyMatch(Qualifier.class::isInstance))).isTrue();
    }

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final FakePersistence persistence = new FakePersistence();
    private final FakeState state = new FakeState();
    private final TokenRevocationService revocations = mock(TokenRevocationService.class);
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
                revocations,
                Clock.fixed(NOW, ZoneOffset.UTC)
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

        String subject = created.subject();
        assertThat(subject).matches("\\d+");
        assertThat(Long.parseLong(subject)).isPositive();
        assertThat(created.oneTimePassword()).matches("T9a[A-Za-z0-9_-]{24}");
        assertThat(persistence.users.get(subject).normalizedUsername())
                .isEqualTo("alice");
        assertThat(persistence.credentials.get(subject).mustChangePassword())
                .isTrue();
        assertThat(state.states).extracting(IdentityUserState::subject)
                .containsExactly(subject);
        assertThat(events).extracting(IdentitySecurityEvent::eventType)
                .containsExactly("IDENTITY_USER_CREATED");
        verify(revocations).revokeSubject(argThat(command -> command.getSubject().equals(subject)
                && command.getReasonCode().equals("USER_CREATED")));
    }

    @Test
    void disablingAndPasswordResetRevokeTheSubjectExactlyOnce() {
        String subject = service.create(new CreateIdentityUserDTO(
                "alice",
                "Alice"
        )).subject();

        IdentityUserVO disabled = service.update(
                subject,
                new UpdateIdentityUserDTO(
                        "Alice Disabled",
                        IdentityUserStatus.DISABLED,
                        0L
                )
        );
        ResetPasswordVO reset =
                service.resetPassword(subject);

        assertThat(disabled.version()).isEqualTo(1L);
        assertThat(reset.oneTimePassword()).matches("T9a[A-Za-z0-9_-]{24}");
        assertThat(persistence.users.get(subject).version())
                .isEqualTo(2L);
        assertThat(persistence.credentials.get(subject).mustChangePassword())
                .isTrue();
        verify(revocations).revokeSubject(argThat(command -> command.getSubject().equals(subject)
                && command.getReasonCode().equals("USER_CREATED")));
        verify(revocations).revokeSubject(argThat(command -> command.getSubject().equals(subject)
                && command.getReasonCode().equals("USER_DISABLED")));
        verify(revocations).revokeSubject(argThat(command -> command.getSubject().equals(subject)
                && command.getReasonCode().equals("PASSWORD_RESET")));
        org.mockito.Mockito.verifyNoMoreInteractions(revocations);
    }

    @Test
    void revokeAllInvokesOnlyTheUnifiedServiceAndPropagatesProjectionFailure() {
        String subject = service.create(new CreateIdentityUserDTO("alice", "Alice")).subject();
        org.mockito.Mockito.clearInvocations(revocations);
        service.revokeAll(subject);
        verify(revocations).revokeSubject(argThat(command -> command.getSubject().equals(subject)
                && command.getReasonCode().equals("ADMIN_REVOKE_ALL")));
        org.mockito.Mockito.verifyNoMoreInteractions(revocations);
        RuntimeException unavailable = new IllegalStateException("PROJECTION_UNAVAILABLE");
        org.mockito.Mockito.doThrow(unavailable).when(revocations).revokeSubject(
                org.mockito.ArgumentMatchers.any());
        int previousEvents = events.size();
        org.junit.jupiter.api.Assertions.assertSame(unavailable,
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                        () -> service.revokeAll(subject)));
        assertThat(events).hasSize(previousEvents);
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

}

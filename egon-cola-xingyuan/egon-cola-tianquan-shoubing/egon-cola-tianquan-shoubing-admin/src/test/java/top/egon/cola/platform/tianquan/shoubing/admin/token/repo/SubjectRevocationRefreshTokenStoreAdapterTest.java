package top.egon.cola.platform.tianquan.shoubing.admin.token.repo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthSubjectRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.port.RefreshTokenStore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubjectRevocationRefreshTokenStoreAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-09T01:00:00Z");

    @Test
    void delegatesOneValidatedSubjectRevocationAndPropagatesFailure() throws Exception {
        TokenRevocationService revocations = mock(TokenRevocationService.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            RefreshTokenStore adapter = adapter(revocations, factory.getValidator());
            adapter.revokeSubject("subject-1001", "PASSWORD_CHANGED", NOW);
            ArgumentCaptor<OAuthSubjectRevokeCommand> command =
                    ArgumentCaptor.forClass(OAuthSubjectRevokeCommand.class);
            verify(revocations).revokeSubject(command.capture());
            assertEquals("subject-1001", command.getValue().getSubject());
            assertEquals("PASSWORD_CHANGED", command.getValue().getReasonCode());
            verifyNoMoreInteractions(revocations);
            RuntimeException failure = new IllegalStateException("PROJECTION_UNAVAILABLE");
            doThrow(failure).when(revocations).revokeSubject(any());
            assertSame(failure, assertThrows(RuntimeException.class,
                    () -> adapter.revokeSubject("subject-1001", "PASSWORD_CHANGED", NOW)));
        }
    }

    @Test
    void rejectsMalformedIdentityAndEveryLegacyTokenLifecycleBeforeEffects() throws Exception {
        TokenRevocationService revocations = mock(TokenRevocationService.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            RefreshTokenStore adapter = adapter(revocations, factory.getValidator());
            for (String subject : new String[]{null, "", "bad subject", "x".repeat(65)}) {
                assertThrows(jakarta.validation.ConstraintViolationException.class,
                        () -> adapter.revokeSubject(subject, "PASSWORD_CHANGED", NOW));
            }
            assertThrows(jakarta.validation.ConstraintViolationException.class,
                    () -> adapter.revokeSubject("subject-1001", "bad reason", NOW));
            assertThrows(NullPointerException.class,
                    () -> adapter.revokeSubject("subject-1001", "PASSWORD_CHANGED", null));
            assertThrows(UnsupportedOperationException.class, () -> adapter.create(null));
            assertThrows(UnsupportedOperationException.class, () -> adapter.findValid("old-digest", NOW));
            assertThrows(UnsupportedOperationException.class,
                    () -> adapter.revokeToken("old-digest", "PASSWORD_CHANGED", NOW));
            assertThrows(UnsupportedOperationException.class, () -> adapter.expire(NOW));
            verifyNoInteractions(revocations);
        }
    }

    @Test
    void existingCorePasswordChangeReachesOnlyTheNewSubjectRevocationAndClearsPasswordBuffers() throws Exception {
        TokenRevocationService revocations = mock(TokenRevocationService.class);
        var users = mock(top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore.class);
        var credentials = mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore.class);
        var hashes = mock(top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort.class);
        var states = mock(top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStatePort.class);
        var events = mock(top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEventPort.class);
        var user = new top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser(
                "subject-1001", "alice", "alice", "Alice",
                top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus.ACTIVE,
                0, null, null, 7L);
        var credential = new top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential(
                "subject-1001", "existing-hash", NOW.minusSeconds(3600), false,
                top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential.Status.ACTIVE, 4L);
        when(users.findById("subject-1001")).thenReturn(java.util.Optional.of(user));
        when(credentials.findActive("subject-1001")).thenReturn(java.util.Optional.of(credential));
        char[] previousPassword = "Previous!Password123".toCharArray();
        char[] nextPassword = "Next!Password45678".toCharArray();
        when(hashes.matches(previousPassword, "existing-hash")).thenReturn(true);
        when(hashes.encode(nextPassword)).thenReturn("changed-hash");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var facade = new top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityFacade(users,
                    credentials, hashes, states, events, adapter(revocations, factory.getValidator()),
                    new top.egon.cola.platform.tianquan.shoubing.core.identity.UsernameNormalizer(),
                    5, java.time.Duration.ofMinutes(5));
            facade.changePassword("subject-1001", previousPassword, nextPassword, NOW);
            verify(revocations).revokeSubject(argThat(command -> command.getSubject().equals("subject-1001")
                    && command.getReasonCode().equals("PASSWORD_CHANGED")));
            verifyNoMoreInteractions(revocations);
            verify(users).save(any(), eq(7L));
            verify(credentials).save(any(), eq(4L));
            verify(states).publish(any());
            verify(events).append(any());
            assertArrayEquals(new char[previousPassword.length], previousPassword);
            assertArrayEquals(new char[nextPassword.length], nextPassword);
        }
    }

    private static RefreshTokenStore adapter(TokenRevocationService service, Validator validator) throws Exception {
        Class<?> type = null;
        try {
            type = Class.forName("top.egon.cola.platform.tianquan.shoubing.admin.token.repo."
                    + "SubjectRevocationRefreshTokenStoreAdapter");
        } catch (ClassNotFoundException missing) {
            // A missing implementation is an expected assertion failure at the RED gate.
        }
        assertNotNull(type, "The subject-only compatibility Adapter is not implemented");
        return (RefreshTokenStore) type.getConstructor(TokenRevocationService.class, Validator.class)
                .newInstance(service, validator);
    }
}

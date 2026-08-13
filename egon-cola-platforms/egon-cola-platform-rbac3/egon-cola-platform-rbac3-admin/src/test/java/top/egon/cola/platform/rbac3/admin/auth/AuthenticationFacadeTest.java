package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import top.egon.cola.platform.rbac3.admin.auth.service.PasswordIdentityAuthenticator;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.auth.repository.CredentialRepository;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.PasswordCredentialVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.exception.AuthenticationFailedException;

class AuthenticationFacadeTest {

    @Test
    void locksAfterFiveFailuresWithoutRevealingWhetherTheUserExists() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        var store = new InMemoryCredentialStore();
        var encoder = new BCryptPasswordEncoder(4);
        store.put(new PasswordCredentialVO(
                "tenant", "user", "user-id", encoder.encode("correct"),
                0, null, true));
        var authenticator = new PasswordIdentityAuthenticator(store, encoder);
        LoginRequest wrong = request("user", "wrong");

        for (int attempt = 0; attempt < 5; attempt++) {
            var error = assertThrows(
                    AuthenticationFailedException.class,
                    () -> authenticator.authenticate(wrong, now));
            assertEquals("AUTHENTICATION_FAILED", error.reasonCode());
        }
        var locked = assertThrows(
                AuthenticationFailedException.class,
                () -> authenticator.authenticate(request("user", "correct"), now));
        assertEquals("AUTHENTICATION_FAILED", locked.reasonCode());
        assertEquals(now.plusSeconds(15 * 60), store.values.get("tenant/user").lockedUntil());

        var unknown = assertThrows(
                AuthenticationFailedException.class,
                () -> authenticator.authenticate(request("unknown", "wrong"), now));
        assertEquals("AUTHENTICATION_FAILED", unknown.reasonCode());
    }

    private LoginRequest request(String username, String password) {
        return new LoginRequest("tenant", username, password,
                new LoginRequest.Device("device", "test"));
    }

    private static final class InMemoryCredentialStore
            implements CredentialRepository {
        private final Map<String, PasswordCredentialVO> values =
                new HashMap<>();

        void put(PasswordCredentialVO credential) {
            values.put(credential.tenantCode() + '/' + credential.normalizedUsername(), credential);
        }

        @Override
        public synchronized <T> T withCredential(
                String tenantCode,
                String normalizedUsername,
                java.util.function.Function<PasswordCredentialVO, T> action
        ) {
            String key = tenantCode + '/' + normalizedUsername;
            PasswordCredentialVO current = values.get(key);
            T result = action.apply(current);
            if (current != null) {
                values.put(key, current);
            }
            return result;
        }

        @Override
        public synchronized void save(
                PasswordCredentialVO credential) {
            put(credential);
        }
    }
}

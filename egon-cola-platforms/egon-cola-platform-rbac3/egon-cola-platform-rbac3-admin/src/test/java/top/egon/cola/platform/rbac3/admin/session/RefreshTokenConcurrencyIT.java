package top.egon.cola.platform.rbac3.admin.session;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefreshTokenConcurrencyIT {

    @Test
    void rotatesExactlyOnceAndCompromisesFamilyOnReplay() throws Exception {
        var store = new InMemoryTokenStore();
        String raw = "refresh-token-1";
        store.add(RefreshTokenService.TokenRecord.active(
                "token-1", "tenant", "session", "family", 0,
                RefreshTokenService.hash(raw), Instant.parse("2026-08-01T00:00:00Z")));
        RefreshTokenService service = new RefreshTokenService(store);
        Instant now = Instant.parse("2026-07-30T10:00:00Z");

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.rotate(raw, now));
            var second = executor.submit(() -> service.rotate(raw, now));
            var results = java.util.List.of(first.get().outcome(), second.get().outcome());
            assertEquals(1, results.stream()
                    .filter(value -> value == RefreshTokenService.Outcome.ROTATED).count());
            assertEquals(1, results.stream()
                    .filter(value -> value == RefreshTokenService.Outcome.REPLAY_DETECTED).count());
        }
        assertEquals(RefreshTokenService.FamilyStatus.COMPROMISED,
                store.familyStatus.get("family"));
    }

    private static final class InMemoryTokenStore
            implements RefreshTokenService.RefreshTokenStore {
        private final Map<String, RefreshTokenService.TokenRecord> tokens = new HashMap<>();
        private final Map<String, RefreshTokenService.FamilyStatus> familyStatus =
                new HashMap<>();

        void add(RefreshTokenService.TokenRecord token) {
            tokens.put(token.tokenHash(), token);
        }

        @Override
        public synchronized <T> T withLockedToken(
                String tokenHash,
                java.util.function.Function<RefreshTokenService.TokenRecord, T> action) {
            return action.apply(tokens.get(tokenHash));
        }

        @Override
        public synchronized void rotate(
                RefreshTokenService.TokenRecord oldToken,
                RefreshTokenService.TokenRecord newToken) {
            tokens.put(oldToken.tokenHash(), oldToken);
            tokens.put(newToken.tokenHash(), newToken);
        }

        @Override
        public synchronized void compromiseFamily(String familyId, Instant detectedAt) {
            familyStatus.put(familyId, RefreshTokenService.FamilyStatus.COMPROMISED);
        }
    }
}

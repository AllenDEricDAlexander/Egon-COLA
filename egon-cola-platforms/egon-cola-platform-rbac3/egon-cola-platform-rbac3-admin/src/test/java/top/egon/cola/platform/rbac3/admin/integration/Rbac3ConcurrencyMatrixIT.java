package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Rbac3ConcurrencyMatrixIT {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void independentSessionsCanActivateTheSameCanonicalFamilyConcurrently()
            throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<String>> calls = java.util.stream.IntStream.range(0, 32)
                    .mapToObj(index -> (Callable<String>) () -> activate("session-" + index))
                    .toList();

            List<String> checksums = executor.invokeAll(calls).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception error) {
                            throw new IllegalStateException(error);
                        }
                    })
                    .toList();

            assertEquals(1, checksums.stream().distinct().count());
        }
    }

    @Test
    void concurrentRefreshReplayHasOneWinnerAndCompromisesTheFamily()
            throws Exception {
        InMemoryRefreshStore store = new InMemoryRefreshStore();
        String raw = "refresh-token";
        store.tokens.put(RefreshTokenService.hash(raw),
                RefreshTokenService.TokenRecord.active(
                        "token", "tenant", "session", "family", 0,
                        RefreshTokenService.hash(raw), NOW.plusSeconds(3600)));
        RefreshTokenService service = new RefreshTokenService(store);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> service.rotate(raw, NOW).outcome());
            var right = executor.submit(() -> service.rotate(raw, NOW).outcome());
            List<RefreshTokenService.Outcome> outcomes = List.of(left.get(), right.get());
            assertEquals(1, outcomes.stream()
                    .filter(RefreshTokenService.Outcome.ROTATED::equals).count());
            assertEquals(1, outcomes.stream()
                    .filter(RefreshTokenService.Outcome.REPLAY_DETECTED::equals).count());
        }
        assertEquals(RefreshTokenService.FamilyStatus.COMPROMISED,
                store.families.get("family"));
    }

    private static String activate(String sessionId) {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(role("root"), role("child")),
                List.of(new RoleEdge("root", "child")));
        return new DefaultRoleActivationResolver().resolve(new RoleActivationInput(
                "tenant", "user", sessionId, List.of("child"),
                List.of(new EligibleAssignmentFact(
                        "assignment", "user", "child",
                        EligibleAssignmentFact.Status.ACTIVE,
                        NOW.minusSeconds(60), null)),
                hierarchy, List.of(),
                new AuthorizationRuleFacts(
                        List.of(new AuthorizationRuleFacts.PermissionBinding(
                                "child", "payment:read")),
                        List.of(), List.of(), List.of(), List.of()),
                3L, 5L, 7L, NOW)).snapshot().checksum();
    }

    private static RoleNode role(String id) {
        return new RoleNode(id, "application", id.toUpperCase(), true,
                RoleNode.RiskLevel.LOW, false, null, 100);
    }

    private static final class InMemoryRefreshStore
            implements RefreshTokenService.RefreshTokenStore {
        private final Map<String, RefreshTokenService.TokenRecord> tokens =
                new HashMap<>();
        private final Map<String, RefreshTokenService.FamilyStatus> families =
                new HashMap<>();

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
            families.put(familyId, RefreshTokenService.FamilyStatus.COMPROMISED);
        }
    }
}

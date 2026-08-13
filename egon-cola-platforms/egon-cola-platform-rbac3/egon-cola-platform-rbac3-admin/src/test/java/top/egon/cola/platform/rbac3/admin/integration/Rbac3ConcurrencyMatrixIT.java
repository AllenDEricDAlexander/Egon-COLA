package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
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
import top.egon.cola.platform.rbac3.admin.session.repository.RefreshTokenRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenFamilyStatusEnum;

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
                TokenRecordVO.active(
                        "token", "tenant", "session", "family", 0,
                        RefreshTokenService.hash(raw), NOW.plusSeconds(3600)));
        RefreshTokenService service = new RefreshTokenService(store);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> service.rotate(raw, NOW).outcome());
            var right = executor.submit(() -> service.rotate(raw, NOW).outcome());
            List<RefreshTokenOutcomeEnum> outcomes = List.of(left.get(), right.get());
            assertEquals(1, outcomes.stream()
                    .filter(RefreshTokenOutcomeEnum.ROTATED::equals).count());
            assertEquals(1, outcomes.stream()
                    .filter(RefreshTokenOutcomeEnum.REPLAY_DETECTED::equals).count());
        }
        assertEquals(RefreshTokenFamilyStatusEnum.COMPROMISED,
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
            implements RefreshTokenRepository {
        private final Map<String, TokenRecordVO> tokens =
                new HashMap<>();
        private final Map<String, RefreshTokenFamilyStatusEnum> families =
                new HashMap<>();

        @Override
        public synchronized <T> T withLockedToken(
                String tokenHash,
                java.util.function.Function<TokenRecordVO, T> action) {
            return action.apply(tokens.get(tokenHash));
        }

        @Override
        public synchronized void rotate(
                TokenRecordVO oldToken,
                TokenRecordVO newToken) {
            tokens.put(oldToken.tokenHash(), oldToken);
            tokens.put(newToken.tokenHash(), newToken);
        }

        @Override
        public synchronized void compromiseFamily(String familyId, Instant detectedAt) {
            families.put(familyId, RefreshTokenFamilyStatusEnum.COMPROMISED);
        }
    }
}

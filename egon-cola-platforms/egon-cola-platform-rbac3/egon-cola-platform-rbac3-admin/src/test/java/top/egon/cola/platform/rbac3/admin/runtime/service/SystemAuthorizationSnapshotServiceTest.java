package top.egon.cola.platform.rbac3.admin.runtime.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.AuthorizationContextVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.SystemAuthorizationSnapshotContextInitializationEnum;

class SystemAuthorizationSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T04:00:00Z");

    @Test
    void snapshotContainsOnlyTheRequestedSystemAndIdentityBinding() {
        AuthorizationContextVO context =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 3, 11, false, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) -> context,
                (tenantId, sessionId) -> new SnapshotRecordVO(
                        tenantId, "alice-sub", "101", sessionSnapshot()),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var snapshot = service.snapshot("1", "5001", "gateway-admin", "alice-sub");

        assertThat(snapshot.identitySub()).isEqualTo("alice-sub");
        assertThat(snapshot.systemCode()).isEqualTo("gateway-admin");
        assertThat(snapshot.contextVersion()).isEqualTo(3);
        assertThat(snapshot.permissions()).containsExactly("gateway:release:read");
        assertThat(snapshot.permissions()).noneMatch(permission -> permission.startsWith("ddc:"));
    }

    @Test
    void snapshotRejectsAStoredIdentityFromAnotherIdpSubject() {
        AuthorizationContextVO context =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 3, 11, false, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) -> context,
                (tenantId, sessionId) -> new SnapshotRecordVO(
                        tenantId, "mallory-sub", "101", sessionSnapshot()),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.snapshot(
                "1", "5001", "gateway-admin", "alice-sub"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTHORIZATION_CONTEXT_MISMATCH");
    }

    @Test
    void unactivatedContextCanOnlyUseRbac3RoleActivationEndpoints() {
        AuthorizationContextVO context =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 0, 11, true, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) -> context,
                (tenantId, sessionId) -> {
                    throw new AssertionError("unactivated context must not load a snapshot");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.snapshot("1", "5001", "rbac3-admin", "alice-sub")
                .permissions()).containsExactlyInAnyOrder(
                "system:role-activation:read",
                "system:role-activation:use");
        assertThat(service.snapshot("1", "5001", "mock-backend", "alice-sub")
                .permissions()).isEmpty();
    }

    @Test
    void developmentInitializerReopensTheActivatedContextBeforeLoadingPermissions() {
        AuthorizationContextVO unactivated =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 0, 11, true, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        AuthorizationContextVO activated =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 1, 11, false, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        AtomicInteger openings = new AtomicInteger();
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) ->
                        openings.getAndIncrement() == 0 ? unactivated : activated,
                (tenantId, sessionId) -> new SnapshotRecordVO(
                        tenantId, "alice-sub", "101", sessionSnapshot(7, 1, 11)),
                Clock.fixed(NOW, ZoneOffset.UTC),
                (context, now) ->
                        SystemAuthorizationSnapshotContextInitializationEnum.COMPLETED);

        var snapshot = service.snapshot("1", "5001", "gateway-admin", "alice-sub");

        assertThat(openings).hasValue(2);
        assertThat(snapshot.permissions()).containsExactly("gateway:release:read");
    }

    @Test
    void completedDevelopmentInitializationWaitsForItsSnapshotPublication() {
        AuthorizationContextVO unactivated =
                context(0, true);
        AuthorizationContextVO activated =
                context(1, false);
        AtomicInteger openings = new AtomicInteger();
        AtomicInteger snapshotReads = new AtomicInteger();
        AtomicInteger pauses = new AtomicInteger();
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) ->
                        openings.getAndIncrement() == 0 ? unactivated : activated,
                (tenantId, sessionId) -> {
                    if (snapshotReads.getAndIncrement() == 0) {
                        throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
                    }
                    return new SnapshotRecordVO(
                            tenantId, "alice-sub", "101",
                            sessionSnapshot(7, 1, 11));
                },
                Clock.fixed(NOW, ZoneOffset.UTC),
                (authorizationContext, now) ->
                        SystemAuthorizationSnapshotContextInitializationEnum.COMPLETED,
                duration -> pauses.incrementAndGet());

        var snapshot = service.snapshot("1", "5001", "gateway-admin", "alice-sub");

        assertThat(snapshotReads).hasValue(2);
        assertThat(pauses).hasValue(1);
        assertThat(snapshot.permissions()).containsExactly("gateway:release:read");
    }

    @Test
    void concurrentDevelopmentInitializationWaitsForTheWinningSnapshotPublication() {
        AuthorizationContextVO unactivated =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 0, 11, true, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        AuthorizationContextVO activated =
                new AuthorizationContextVO(
                        "9001", "1", "5001", "alice-sub", "101",
                        7, 1, 11, false, "ACTIVE", NOW.minusSeconds(10),
                        NOW.plusSeconds(3600));
        AtomicInteger openings = new AtomicInteger();
        AtomicInteger snapshotReads = new AtomicInteger();
        AtomicInteger pauses = new AtomicInteger();
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) ->
                        openings.getAndIncrement() == 0 ? unactivated : activated,
                (tenantId, sessionId) -> {
                    if (snapshotReads.getAndIncrement() == 0) {
                        return new SnapshotRecordVO(
                                tenantId, "alice-sub", "101",
                                sessionSnapshot(7, 0, 11));
                    }
                    return new SnapshotRecordVO(
                            tenantId, "alice-sub", "101",
                            sessionSnapshot(7, 1, 11));
                },
                Clock.fixed(NOW, ZoneOffset.UTC),
                (context, now) ->
                        SystemAuthorizationSnapshotContextInitializationEnum.CONCURRENT,
                duration -> pauses.incrementAndGet());

        var snapshot = service.snapshot("1", "5001", "gateway-admin", "alice-sub");

        assertThat(snapshotReads).hasValue(2);
        assertThat(pauses).hasValue(1);
        assertThat(snapshot.permissions()).containsExactly("gateway:release:read");
    }

    @Test
    void concurrentDevelopmentInitializationFailsClosedWhenSnapshotStaysStale() {
        AuthorizationContextVO unactivated =
                context(0, true);
        AuthorizationContextVO activated =
                context(1, false);
        AtomicInteger openings = new AtomicInteger();
        AtomicInteger snapshotReads = new AtomicInteger();
        AtomicInteger pauses = new AtomicInteger();
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) ->
                        openings.getAndIncrement() == 0 ? unactivated : activated,
                (tenantId, sessionId) -> {
                    snapshotReads.incrementAndGet();
                    return new SnapshotRecordVO(
                            tenantId, "alice-sub", "101",
                            sessionSnapshot(7, 0, 11));
                },
                Clock.fixed(NOW, ZoneOffset.UTC),
                (authorizationContext, now) ->
                        SystemAuthorizationSnapshotContextInitializationEnum.CONCURRENT,
                duration -> pauses.incrementAndGet());

        assertThatThrownBy(() -> service.snapshot(
                "1", "5001", "gateway-admin", "alice-sub"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTH_PROPAGATION_PENDING");
        assertThat(snapshotReads).hasValue(20);
        assertThat(pauses).hasValue(19);
    }

    @Test
    void concurrentDevelopmentInitializationDoesNotRetryUnrelatedFailures() {
        AtomicInteger openings = new AtomicInteger();
        AtomicInteger snapshotReads = new AtomicInteger();
        AtomicInteger pauses = new AtomicInteger();
        SystemAuthorizationSnapshotService service = new SystemAuthorizationSnapshotService(
                (tenantId, sessionId, identitySub, now, expiresAt) ->
                        openings.getAndIncrement() == 0
                                ? context(0, true)
                                : context(1, false),
                (tenantId, sessionId) -> {
                    snapshotReads.incrementAndGet();
                    throw new Rbac3RuleViolation("AUTHORIZATION_CONTEXT_MISMATCH");
                },
                Clock.fixed(NOW, ZoneOffset.UTC),
                (authorizationContext, now) ->
                        SystemAuthorizationSnapshotContextInitializationEnum.CONCURRENT,
                duration -> pauses.incrementAndGet());

        assertThatThrownBy(() -> service.snapshot(
                "1", "5001", "gateway-admin", "alice-sub"))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTHORIZATION_CONTEXT_MISMATCH");
        assertThat(snapshotReads).hasValue(1);
        assertThat(pauses).hasValue(0);
    }

    private AuthorizationContextVO context(
            long contextVersion,
            boolean activationRequired) {
        return new AuthorizationContextVO(
                "9001", "1", "5001", "alice-sub", "101",
                7, contextVersion, 11, activationRequired, "ACTIVE",
                NOW.minusSeconds(10), NOW.plusSeconds(3600));
    }

    private SessionAuthorizationSnapshot sessionSnapshot() {
        return sessionSnapshot(7, 3, 11);
    }

    private SessionAuthorizationSnapshot sessionSnapshot(
            long authVersion,
            long sessionVersion,
            long policyVersion) {
        return new SessionAuthorizationSnapshot(
                "5001", authVersion, sessionVersion, policyVersion,
                List.of(
                        app("gateway-admin", Set.of("gateway:release:read")),
                        app("ddc-admin", Set.of("ddc:config:read"))),
                "sha256:all", NOW);
    }

    private AppAuthorizationContext app(String code, Set<String> permissions) {
        return new AppAuthorizationContext(
                code + "-id", code, List.of("role-1"), List.of("assignment-1"),
                List.of("role-1"), permissions, Map.of(), Map.of(), List.of(), null);
    }
}

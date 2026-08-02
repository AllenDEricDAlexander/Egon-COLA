package top.egon.cola.platform.rbac3.admin.snapshot.application;

import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the non-business capability snapshot required immediately after login. */
public final class LoginRuntimeProjectionFactory {

    private static final String RBAC3_APPLICATION_CODE = "rbac3-admin";
    private static final Set<String> SESSION_CAPABILITIES = Set.of(
            "system:role-activation:read",
            "system:role-activation:use",
            "system:session:logout");

    public SessionSnapshotProjector.Projection create(
            SessionFacade.SessionRecord session,
            Instant generatedAt) {
        return create(new RuntimeState(
                session.tenantId(), session.userId(), session.sessionId(),
                session.status().name(), session.authVersion(), session.sessionVersion(),
                session.policyVersion(), session.absoluteExpiresAt()), generatedAt);
    }

    public SessionSnapshotProjector.Projection create(
            RuntimeState session,
            Instant generatedAt) {
        List<AppAuthorizationContext> contexts = "ACTIVE".equals(session.status())
                ? List.of(new AppAuthorizationContext(
                session.tenantId(), RBAC3_APPLICATION_CODE,
                List.of(), List.of(), List.of(), SESSION_CAPABILITIES,
                Map.of(), Map.of(), List.of(), null))
                : List.of();
        SessionAuthorizationSnapshot snapshot = new SessionAuthorizationSnapshot(
                session.sessionId(), session.authVersion(), session.sessionVersion(),
                session.policyVersion(), contexts, checksum(session), generatedAt);
        SessionSnapshotProjector.RuntimeSession runtimeSession =
                new SessionSnapshotProjector.RuntimeSession(
                        session.tenantId(), session.userId(), session.userId(),
                        session.sessionId(),
                        session.status(), session.authVersion(),
                        session.sessionVersion(), session.policyVersion(),
                        session.absoluteExpiresAt());
        return new SessionSnapshotProjector.Projection(runtimeSession, snapshot);
    }

    private String checksum(RuntimeState session) {
        String canonical = "session|" + session.tenantId() + '|' + session.userId()
                + '|' + session.sessionId() + '|' + session.authVersion()
                + '|' + session.sessionVersion() + '|' + session.policyVersion()
                + '|' + session.status() + '|'
                + ("ACTIVE".equals(session.status())
                ? SESSION_CAPABILITIES.stream().sorted().toList() : List.of());
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(
                            canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record RuntimeState(
            String tenantId,
            String userId,
            String sessionId,
            String status,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Instant absoluteExpiresAt
    ) {
    }
}

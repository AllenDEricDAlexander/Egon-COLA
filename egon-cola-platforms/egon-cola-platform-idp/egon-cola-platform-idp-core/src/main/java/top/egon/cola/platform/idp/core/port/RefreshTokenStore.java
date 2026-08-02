package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.token.RefreshFamily;

import java.time.Instant;

public interface RefreshTokenStore {

    void create(RefreshFamily family);

    RotationResult rotate(RotationCommand command);

    void revokeFamily(String familyId, String reason, Instant now);

    void revokeSubject(String identitySub, String reason, Instant now);

    record RotationCommand(
            String familyId,
            String currentTokenDigest,
            String successorTokenDigest,
            long successorGeneration,
            String expectedIdentitySub,
            long expectedTokenVersion,
            Instant expiresAt,
            Instant now
    ) {
    }

    record RotationResult(
            RotationOutcome outcome,
            RefreshFamily family
    ) {
    }

    enum RotationOutcome {
        ROTATED,
        REPLAY,
        REVOKED,
        MISSING
    }
}

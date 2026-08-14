package top.egon.cola.platform.idp.core.token;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StableRefreshTokenTest {

    @Test
    void inProcessPairRedactsBothRawTokens() {
        UserTokenPair pair = new UserTokenPair(
                "access-secret",
                "refresh-secret",
                Instant.parse("2026-08-02T00:05:00Z"),
                Instant.parse("2026-08-09T00:00:00Z"));

        String description = pair.toString();

        assertTrue(description.contains("accessToken=<redacted>"));
        assertTrue(description.contains("refreshToken=<redacted>"));
        assertFalse(description.contains("access-secret"));
        assertFalse(description.contains("refresh-secret"));
    }

    @Test
    void refreshRecordContainsOnlyDigestAndStableAbsoluteExpiry() {
        RefreshTokenRecord record = new RefreshTokenRecord(
                "digest",
                "subject",
                "tenant",
                Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-08-09T00:00:00Z"),
                RefreshTokenRecord.Status.ACTIVE);

        assertFalse(record.tokenDigest().equals("refresh-secret"));
        assertTrue(record.expiresAt().isAfter(record.issuedAt()));
    }
}

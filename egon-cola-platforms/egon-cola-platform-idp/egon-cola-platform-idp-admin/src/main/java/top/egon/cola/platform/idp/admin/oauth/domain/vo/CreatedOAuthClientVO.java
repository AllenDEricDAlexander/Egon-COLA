package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import java.time.Instant;

/**
 * One-time response returned after an administrator creates an OAuth Client.
 *
 * <p>The plaintext Secret is request-scoped and is never persisted or included in the
 * redacted textual representation.</p>
 */
public record CreatedOAuthClientVO(
        String clientId,
        String appId,
        String clientName,
        String clientType,
        String status,
        String clientSecret,
        String secretHint,
        long version,
        Instant createdAt
) {

    @Override
    public String toString() {
        return "CreatedOAuthClientVO[clientId=" + clientId
                + ", appId=" + appId
                + ", clientName=" + clientName
                + ", clientType=" + clientType
                + ", status=" + status
                + ", clientSecret=<redacted>"
                + ", secretHint=" + secretHint
                + ", version=" + version
                + ", createdAt=" + createdAt
                + ']';
    }
}

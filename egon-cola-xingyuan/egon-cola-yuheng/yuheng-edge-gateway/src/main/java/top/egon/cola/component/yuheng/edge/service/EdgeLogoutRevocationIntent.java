package top.egon.cola.component.yuheng.edge.service;

import java.time.Instant;

public record EdgeLogoutRevocationIntent(
        String intentId,
        String registrationId,
        String issuer,
        String tenantId,
        String clientId,
        String tokenValue,
        String tokenTypeHint,
        Instant createdAt) {

    public EdgeLogoutRevocationIntent {
        if (intentId == null || intentId.isBlank()
                || registrationId == null || registrationId.isBlank()
                || issuer == null || issuer.isBlank()
                || tenantId == null
                || !("PLATFORM".equals(tenantId) || tenantId.matches("[1-9][0-9]*"))
                || clientId == null || clientId.isBlank()
                || tokenValue == null || tokenValue.isBlank()
                || !"refresh_token".equals(tokenTypeHint)
                || createdAt == null) {
            throw new IllegalArgumentException("Edge logout revocation intent is invalid");
        }
    }

    @Override
    public String toString() {
        return "EdgeLogoutRevocationIntent[intentId=" + intentId
                + ", registrationId=" + registrationId
                + ", issuer=" + issuer
                + ", tenantId=" + tenantId
                + ", clientId=" + clientId
                + ", tokenValue=<redacted>, tokenTypeHint=" + tokenTypeHint
                + ", createdAt=" + createdAt + "]";
    }
}

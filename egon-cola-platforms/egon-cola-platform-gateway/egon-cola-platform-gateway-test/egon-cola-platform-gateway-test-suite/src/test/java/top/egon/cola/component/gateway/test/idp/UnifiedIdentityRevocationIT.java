package top.egon.cola.component.gateway.test.idp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedIdentityRevocationIT {

    @Test
    void revokedIdentityIsRejectedBeforeTheBackend() throws Exception {
        UnifiedIdentityLiveClient client = UnifiedIdentityLiveClient.enabled();

        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_GATEWAY_URL"),
                "/api/mock/read",
                client.token("UNIFIED_IDENTITY_REVOKED_TOKEN_FILE")))
                .isEqualTo(401);
        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_GATEWAY_URL"),
                "/api/mock/read",
                client.token("UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE")))
                .isEqualTo(200);
    }
}
